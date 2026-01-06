package com.dataflow.dataingestionservice.bt.service;

import com.dataflow.dataingestionservice.Models.Currency;
import com.dataflow.dataingestionservice.Models.Expense;
import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Repositories.CurrencyRepository;
import com.dataflow.dataingestionservice.Repositories.ExpenseRepository;
import com.dataflow.dataingestionservice.Repositories.TransactionRepository;
import com.dataflow.dataingestionservice.bt.config.BtApiProperties;
import com.dataflow.dataingestionservice.bt.model.BankAccount;
import com.dataflow.dataingestionservice.bt.model.UserBtDetail;
import com.dataflow.dataingestionservice.bt.repository.BankAccountRepository;
import com.dataflow.dataingestionservice.bt.repository.UserBtDetailRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AccountSyncService {
    private final RestTemplate restTemplate =  new RestTemplate();
    private final BtApiProperties props;
    private final ObjectMapper objectMapper;
    private final UserBtDetailRepository btUserDetailRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final ExpenseRepository expenseRepository;
    private final CurrencyRepository currencyRepository;

    private final AuthService authService;
    private final DateTimeFormatter[] POSSIBLE_DATE_FORMATS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    public AccountSyncService( BtApiProperties props,
                               ObjectMapper objectMapper,
                               UserBtDetailRepository btUserDetailRepository,
                               BankAccountRepository bankAccountRepository,
                               TransactionRepository transactionRepository,
                               ExpenseRepository expenseRepository,
                               CurrencyRepository currencyRepository,
                               AuthService authService) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.btUserDetailRepository = btUserDetailRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.expenseRepository = expenseRepository;
        this.currencyRepository = currencyRepository;
        this.authService = authService;
    }

    /**
     * Sync a single account for the user. This will:
     * - page through the transactions endpoint
     * - refresh token on 401 and retry once
     * - dedupe by bt_transaction_id
     * - save positive amounts as Transaction and (optionally) outgoing as Expense depending on your rule
     *
     * @param account the BankAccount entity (must include account id used by the BT API and userId)
     * @param userBtDetail token info for this user/consent
     * @param initialFromDate if null -> will default to now().minusDays(90) for initial full sync
     */
    @Transactional
    @Async
    public void syncAccount(BankAccount account, UserBtDetail userBtDetail, LocalDate initialFromDate){
        final int limit = 100;
        int page = 1;

        LocalDate from = initialFromDate != null ? initialFromDate : LocalDate.now().minusDays(90);
        LocalDate to = LocalDate.now();

        boolean morePages = true;
        while(morePages){
            ResponseEntity<JsonNode> response = callTransactionsApiWithRetry(account, userBtDetail, from, to, page, limit);

            if(response == null){
                System.out.println("Sync failed for account " + account.getIban());
                return;
            }
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                System.out.println("Non-success response while syncing account " + account.getIban() + ": " + response);
                return;
            }

            JsonNode root = response.getBody();
            JsonNode transactionArray = root.path("transactions").path("booked");

            if(transactionArray == null || !transactionArray.isArray() || transactionArray.isEmpty()){
                morePages = false;
            }else{
                processTransactionNodes(transactionArray, account, userBtDetail);

                if(transactionArray.size() < limit){
                    morePages = false;
                }else{
                    page++;
                }
            }

            account.setLastSyncDate(LocalDateTime.now());
            bankAccountRepository.save(account);
            System.out.println("Sync finished for" + account.getIban());

        }
    }

    /**
     * Calls transactions API and refreshes token if we get 401. Returns null if unrecoverable.
     */
    private ResponseEntity<JsonNode> callTransactionsApiWithRetry(BankAccount account,
                                                                  UserBtDetail userBtDetail,
                                                                  LocalDate dateFrom,
                                                                  LocalDate dateTo,
                                                                  int page,
                                                                  int limit) {
        ResponseEntity<JsonNode> response = callTransactionsApi(account, userBtDetail, dateFrom, dateTo, page, limit);

        if (response != null && response.getStatusCode().value() == 401) {
            // try refresh once
            System.out.println("401 received - refreshing token for user " + userBtDetail.getUserId());
            boolean refreshed = authService.refreshAccessToken(userBtDetail);
            if (!refreshed) {
                System.out.println("Token refresh failed for user " + userBtDetail.getUserId());
                return null;
            }
            // retry once
            response = callTransactionsApi(account, userBtDetail, dateFrom, dateTo, page, limit);
        }
        return response;
    }

    /**
     * Low level call to BT transactions endpoint
     */
    private ResponseEntity<JsonNode> callTransactionsApi(BankAccount account,
                                                         UserBtDetail userBtDetail,
                                                         LocalDate dateFrom,
                                                         LocalDate dateTo,
                                                         int page,
                                                         int limit) {
        try {
            String dateFromStr = formatDateForApi(dateFrom);
            String dateToStr = formatDateForApi(dateTo);

            String url = String.format("%sbt-psd2-aisp/v2/accounts/%s/transactions?bookingStatus=booked&dateFrom=%s&dateTo=%s&page=%d&limit=%d",
                    props.getApiBase(), account.getResourceId(), dateFromStr, dateToStr, page, limit);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Request-ID", UUID.randomUUID().toString());
            headers.set("PSU-IP-Address", props.getMyIp());
            headers.set("PSU-Geo-Location", props.getGeoLocation());
            headers.set("Consent-ID", userBtDetail.getConsentId());
            headers.setBearerAuth(userBtDetail.getAccessToken()); // Authorization: Bearer <token>

            HttpEntity<Void> req = new HttpEntity<>(headers);
            return restTemplate.exchange(url, HttpMethod.GET, req, JsonNode.class);
        }catch (HttpClientErrorException e) {

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // propagate a 401-like response to upper layer
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(e.getResponseBodyAs(JsonNode.class));
            }

            throw e; // rethrow other 4xx errors
        } catch (Exception e) {
            System.out.println("HTTP call failed: " + e.getMessage());
            return null;
        }
    }

    private String formatDateForApi(LocalDate d) {
        return d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }


    /**
     * Process an array of transaction JSON nodes: map, dedupe and save
     */
    private void processTransactionNodes(
            JsonNode transactionsArray,
            BankAccount account,
            UserBtDetail userBtDetail
    ){
        List<String> btIds = new ArrayList<>();
        for (JsonNode txNode : transactionsArray) {
            if (txNode.hasNonNull("transactionId")) {
                btIds.add(txNode.get("transactionId").asText());
            }
        }

        // fetch existing bt ids in DB to avoid duplicates (batch)
        Set<String> existingTxIds = new HashSet<>(transactionRepository.getTransactionsIdsByBtTransactionId(btIds, userBtDetail.getUserId()));
        Set<String> existingExpenseIds = new HashSet<>(expenseRepository.getExpensesIdsByBtTransactionId(btIds, userBtDetail.getUserId()));
        // now iterate and save only new ones
        List<Transaction> txsToSave = new ArrayList<>();
        List<Expense> expsToSave = new ArrayList<>();

        //create counter for syntetic time as the api returns LocalDate with no time
        Map<LocalDate, Integer> counters = new HashMap<>();

        for (JsonNode txNode : transactionsArray) {
            String btId = txNode.path("transactionId").asText(null);
            if (btId == null) continue;
            if (existingTxIds.contains(btId) || existingExpenseIds.contains(btId)) {
                // already have this transaction
                continue;
            }

            // parse amount & currency
            JsonNode amountNode = txNode.path("transactionAmount");
            String amountText = amountNode.path("amount").asText();
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountText);
            } catch (Exception ex) {
                System.out.println("Invalid amount format for tx " + btId + ": " + amountText);
                continue;
            }
            String currencyCode = amountNode.path("currency").asText(null);
            Currency currencyEntity = currencyCode != null ? currencyRepository.findByCodeContainingIgnoreCase(currencyCode) : null;

            String transactionDetails = txNode.path("details").asText();

            // parse date
            String bookingDateStr = txNode.path("bookingDate").asText(null);
            LocalDate bookingDate = parseDateFlexible(bookingDateStr);

            int counter = counters.getOrDefault(bookingDate, 0);
            counters.put(bookingDate, counter + 1);

            LocalDateTime syntheticDateTime = bookingDate.atStartOfDay().plusSeconds(counter);

            // description
            String details = txNode.path("details").asText(null);

            //positive -> income
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                Transaction t = new Transaction();
                t.setId(UUID.randomUUID().toString());
                t.setBtTransactionId(btId);
                t.setUserId(userBtDetail.getUserId());
                t.setTransactionDate(syntheticDateTime);
                t.setAmount(amount);
                t.setDescription(details);
                t.setCurrency(currencyEntity);
                t.setCategory("bank");
                t.setDescription(transactionDetails);
                t.setPaymentMode(null);
                t.setCreatedAt(LocalDateTime.now());

                txsToSave.add(t);
            } else {
                // amount == 0  income; <=0 expense
                Expense e = new Expense();
                e.setId(UUID.randomUUID().toString());
                e.setBtTransactionId(btId);
                e.setUserId(userBtDetail.getUserId());
                e.setDueDate(null);
                e.setAmount(amount.negate());
                e.setCurrency(currencyEntity);
                e.setDescription(details);
                e.setExpenseType("bank");
                expsToSave.add(e);
            }
        }

        // bulk save (filtered for duplicates)
        if (!txsToSave.isEmpty()) {
            transactionRepository.saveAll(txsToSave);
            System.out.println("Saved " + txsToSave.size() + " transactions for account " + account.getIban());
        }
        if (!expsToSave.isEmpty()) {
            expenseRepository.saveAll(expsToSave);
            System.out.println("Saved " + expsToSave.size() + " expenses for account " + account.getIban());
        }

        int id = bankAccountRepository.updateBankAccountById(account.getId(), LocalDateTime.now());
        if(id == 0){
            System.out.println("Bank account wasn't updated, ID: " + account.getId());
        }

    }


    private LocalDate parseDateFlexible(String value) {
        if (value == null) return null;
        for (DateTimeFormatter f : POSSIBLE_DATE_FORMATS) {
            try {
                return LocalDate.parse(value, f);
            } catch (Exception ignore) {}
        }
        // fallback try parsing numeric epoch-ish or substring
        try {
            return LocalDate.parse(value);
        } catch (Exception ignore) {}
        return null;
    }



}
