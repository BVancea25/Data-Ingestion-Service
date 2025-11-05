package com.dataflow.dataingestionservice.bt.service;

import com.dataflow.dataingestionservice.Utils.SecurityUtils;
import com.dataflow.dataingestionservice.bt.config.BtApiProperties;
import com.dataflow.dataingestionservice.bt.model.UserBtDetail;
import com.dataflow.dataingestionservice.bt.repository.UserBtDetailRepository;
import com.dataflow.dataingestionservice.bt.util.PkceUtil;
import org.apache.commons.collections4.MultiValuedMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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


    private UserBtDetailRepository btUserDetailRepository;
    public BtService(BtApiProperties props, UserBtDetailRepository btUserDetailRepository){
        this.props = props;
        this.btUserDetailRepository = btUserDetailRepository;
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
            btUserDetailRepository.save(userBtDetail);

            System.out.println("Got tokens");

        }else{
            throw new RuntimeException("Failed to retrieve auth tokens");
        }
    }



}
