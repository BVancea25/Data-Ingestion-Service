package com.dataflow.dataingestionservice.bt.service;

import com.dataflow.dataingestionservice.Models.Currency;
import com.dataflow.dataingestionservice.Repositories.CurrencyRepository;
import com.dataflow.dataingestionservice.Utils.SecurityUtils;
import com.dataflow.dataingestionservice.bt.config.BtApiProperties;
import com.dataflow.dataingestionservice.bt.dto.BankAccountDTO;
import com.dataflow.dataingestionservice.bt.model.BankAccount;
import com.dataflow.dataingestionservice.bt.model.UserBtDetail;
import com.dataflow.dataingestionservice.bt.repository.BankAccountRepository;
import com.dataflow.dataingestionservice.bt.repository.UserBtDetailRepository;
import com.dataflow.dataingestionservice.bt.util.PkceUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import netscape.javascript.JSObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;


@Service
public class BtService {
    private final BtApiProperties props;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final UserBtDetailRepository btUserDetailRepository;
    private final CurrencyRepository currencyRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountSyncService accountSyncService;
    private final AuthService authService;
    public BtService(BtApiProperties props,
                     UserBtDetailRepository btUserDetailRepository,
                     CurrencyRepository currencyRepository,
                     BankAccountRepository bankAccountRepository,
                     ObjectMapper objectMapper,
                     AuthService authService,
                     AccountSyncService accountSyncService){
        this.props = props;
        this.btUserDetailRepository = btUserDetailRepository;
        this.currencyRepository = currencyRepository;
        this.objectMapper = objectMapper;
        this.bankAccountRepository = bankAccountRepository;
        this.authService = authService;
        this.accountSyncService = accountSyncService;
    }

    public String createConsentAndBuildRedirect(){
        UserBtDetail userBtDetail = createConsent();

        String codeVerifier = PkceUtil.generateCodeVerifier();
        String codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier);

        userBtDetail.setCodeVerifier(codeVerifier);


        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        userBtDetail.setState(state);
        btUserDetailRepository.save(userBtDetail);

        String redirectUrl = String.format(
                "%s?response_type=code&client_id=%s&redirect_uri=%s&scope=AIS:%s&state=%s&nonce=%s&code_challenge=%s&code_challenge_method=S256",
                props.getOauthBase(),
                props.getClientId(),
                props.getRedirectUri(),
                userBtDetail.getConsentId(),
                state,
                nonce,
                codeChallenge
        );

        return redirectUrl;
    }
    public UserBtDetail createConsent(){
        String url = props.getApiBase() + "/bt-psd2-aisp/v2/consents";

        LocalDate validUntill = LocalDate.now().plusDays(150);

        Map<String, Object> body = new HashMap<>();
        body.put("access",Map.of("availableAccounts","allAccounts"));
        body.put("recurringIndicator", true);
        body.put("validUntil", validUntill.toString());
        body.put("combinedServiceIndicator", false);
        body.put("frequencyPerDay", 4);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("TPP-Redirect-URI", props.getRedirectUri());
        headers.set("X-Request-ID", UUID.randomUUID().toString());
        headers.set("PSU-IP-Address", props.getMyIp());
        headers.set("PSU-Geo-Location", props.getGeoLocation());

        HttpEntity<Map<String,Object>> request = new HttpEntity<>(body, headers);
        System.out.println(url);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null){
            Map<String, Object> bodyMap = response.getBody();
            String consentId = (String) bodyMap.get("consentId");
            String consentStatus = (String) bodyMap.get("consentStatus");

            UserBtDetail userBtDetail = new UserBtDetail();
            userBtDetail.setConsentId(consentId);
            userBtDetail.setUserId(SecurityUtils.getCurrentUserUuid());
            userBtDetail.setValidUntill(validUntill);
            userBtDetail.setConsentStatus(consentStatus);

            return userBtDetail;
        }else{
            throw new RuntimeException("Failed to create consent with BT API");
        }
    }

    public void exchangeCodeForTokens(String code, String state){


        UserBtDetail userBtDetail = btUserDetailRepository.findUserBtDetailByState(state);
        System.out.println("GOT CODE: " + code);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.put("code", Collections.singletonList(code));
        body.put("grant_type", Collections.singletonList("authorization_code"));
        body.put("redirect_uri", Collections.singletonList(props.getRedirectUri()));
        body.put("client_id", Collections.singletonList(props.getClientId()));
        body.put("client_secret", Collections.singletonList(props.getClientSecret()));
        body.put("code_verifier", Collections.singletonList(userBtDetail.getCodeVerifier()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String,String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(props.getTokenUrl(), HttpMethod.POST, request, Map.class);

        if(response.getStatusCode().is2xxSuccessful()){
            Map<String, Object> bodyMap = response.getBody();

            System.out.println("Full token response: " + bodyMap);

            String accessToken = (String) bodyMap.get("access_token");
            String refreshToken = (String) bodyMap.get("refresh_token");

            userBtDetail.setAccessToken(accessToken);
            userBtDetail.setRefreshToken(refreshToken);
            userBtDetail.setCode(code);
            userBtDetail.setConsentStatus("valid");
            btUserDetailRepository.save(userBtDetail);

            getBankAccountDetails(userBtDetail);
            System.out.println("Got tokens");

        }else{
            throw new RuntimeException("Failed to retrieve auth tokens");
        }
    }

    public void getBankAccountDetails(UserBtDetail userBtDetail) {
        String status = userBtDetail.getConsentStatus();

        if (!"valid".equals(status)) {
            System.out.println("Consent is not valid: " + status);
            return;
        }

        try {
            // Try once, retry once if unauthorized
            boolean success = fetchAccountDetails(userBtDetail, false);

            if (!success) {
                System.out.println("Initial request failed, attempting token refresh...");
                if (authService.refreshAccessToken(userBtDetail)) {
                    fetchAccountDetails(userBtDetail, true);
                } else {
                    System.out.println("Failed to refresh token — aborting.");
                }
            }

        } catch (Exception e) {
            System.err.println("Unexpected error while fetching account details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean fetchAccountDetails(UserBtDetail userBtDetail, boolean isRetry) {
        String url = props.getApiBase() + "bt-psd2-aisp/v2/accounts";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Request-ID", UUID.randomUUID().toString());
        headers.set("PSU-IP-Address", props.getMyIp());
        headers.set("PSU-Geo-Location", props.getGeoLocation());
        headers.set("Consent-ID", userBtDetail.getConsentId());
        headers.set("Authorization", "Bearer " + userBtDetail.getAccessToken());

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println((isRetry ? "[Retry]" : "") + " Got account data: " + response.getBody());
                TypeFactory typeFactory = objectMapper.getTypeFactory();

                JsonNode accountsNode = response.getBody().get("accounts");


                if (accountsNode != null && accountsNode.isArray()) {
                    List<BankAccountDTO> bankAccountDTOList = objectMapper.treeToValue(accountsNode, typeFactory.constructCollectionType(List.class, BankAccountDTO.class));

                    for (int i = 0; i < accountsNode.size(); i++) {
                        
                        BankAccountDTO newBankAccountDTO = bankAccountDTOList.get(i);

                        BankAccount bankAccountExists = bankAccountRepository.getBankAccountByResourceId(newBankAccountDTO.getResourceId());
                        if(bankAccountExists == null) {
                            Currency currency = currencyRepository.findByCodeContainingIgnoreCase(newBankAccountDTO.getCurrency());


                            BankAccount bankAccount = new BankAccount();
                            bankAccount.setUserBtDetail(userBtDetail);
                            bankAccount.setCurrency(currency);
                            bankAccount.setIban(newBankAccountDTO.getIban());
                            bankAccount.setName(newBankAccountDTO.getName());
                            bankAccount.setResourceId(newBankAccountDTO.getResourceId());

                            bankAccountRepository.save(bankAccount);
                            bankAccountExists = bankAccount;
                        }

                        accountSyncService.syncAccount(bankAccountExists, userBtDetail, null);
                    }

                }else{
                    System.out.println("Couldn't parse JSON");
                }




                return true;
            } else if (response.getStatusCode().value() == 401) {
                // Token invalid
                System.out.println("Access token expired or invalid. Need refresh.");
                return false;
            } else {
                System.out.println("Error fetching accounts: " + response.getStatusCode() + " - " + response.getBody());
                return true; // No retry needed, error is not auth-related
            }

        } catch (Exception e) {
            System.err.println("Error in account fetch: " + e.getMessage());
            return false;
        }
    }


    public String getValidConsent() {
        String userId = SecurityUtils.getCurrentUserUuid();
        UserBtDetail userBtDetail = btUserDetailRepository.findUserBtDetailByConsentStatusAndUserId("valid", userId);
        LocalDate validUntill = null;

        if(userBtDetail != null){
            validUntill = userBtDetail.getValidUntill();
        }
        else{
            return "not_found";
        }

        if(validUntill != null && validUntill.isBefore(LocalDate.now()) ){
            return "expired";
        }

        return "valid";
    }
}
