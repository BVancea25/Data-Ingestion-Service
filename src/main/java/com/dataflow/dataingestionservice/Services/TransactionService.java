package com.dataflow.dataingestionservice.Services;

import com.dataflow.dataingestionservice.Config.ServiceProperties.ReportingServiceApiProperties;
import com.dataflow.dataingestionservice.DTO.FactTransactionDTO;
import com.dataflow.dataingestionservice.DTO.TransactionDTO;
import com.dataflow.dataingestionservice.DTO.TransactionFilter;
import com.dataflow.dataingestionservice.DTO.UpdateTransactionDTO;
import com.dataflow.dataingestionservice.Models.Category;
import com.dataflow.dataingestionservice.Models.Currency;
import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Repositories.CategoryRepository;
import com.dataflow.dataingestionservice.Repositories.CurrencyRepository;
import com.dataflow.dataingestionservice.Repositories.TransactionRepository;
import com.dataflow.dataingestionservice.Specifications.TransactionSpecifications;
import com.dataflow.dataingestionservice.Utils.Constants.SyncOperation;
import com.dataflow.dataingestionservice.Utils.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CurrencyRepository currencyRepository;
    private final CategoryRepository categoryRepository;
    private final RestTemplate restTemplate;

    private final ReportingServiceApiProperties reportingServiceApiProperties;
    public TransactionService(TransactionRepository transactionRepository,
                              CurrencyRepository currencyRepository, CategoryRepository categoryRepository,
                              ReportingServiceApiProperties reportingServiceApiProperties,
                              RestTemplate restTemplate) {
        this.transactionRepository = transactionRepository;
        this.currencyRepository = currencyRepository;
        this.categoryRepository = categoryRepository;
        this.reportingServiceApiProperties = reportingServiceApiProperties;
        this.restTemplate = restTemplate;

    }

    @Transactional
    public void saveTransactions(List<TransactionDTO> transactionDTOs) {
        if (transactionDTOs == null || transactionDTOs.isEmpty()) return;

        String userId = SecurityUtils.getCurrentUserUuid();

        List<Transaction> entities = transactionDTOs.stream()
                .map(dto -> {
                    Transaction tx = new Transaction();

                    String id = dto.getId() != null
                            ? dto.getId()
                            : UUID.randomUUID().toString();

                    tx.setId(id);
                    tx.setUserId(userId);
                    tx.setAmount(dto.getAmount());
                    //tx.setCategory(dto.getCategory());
                    tx.setTransactionDate(dto.getTransactionDate());
                    tx.setPaymentMode(dto.getPaymentMode());
                    tx.setDescription(dto.getDescription());
                    tx.setType(dto.getType());

                    Optional<Category> category = categoryRepository
                            .findById(dto.getCategoryId());
                    category.ifPresent(tx::setCategory);

                    Currency currency = currencyRepository
                            .findByCodeContainingIgnoreCase(dto.getCurrencyCode());
                    tx.setCurrency(currency);

                    return tx;
                })
                .toList();

        transactionRepository.saveAll(entities);

        List<FactTransactionDTO> factDtos = entities.stream()
                .map(tx -> mapToFactTransactionDTO(tx, SyncOperation.CREATE))
                .toList();

        sendToReportingService(factDtos);
    }

    @Transactional
    public void updateTransaction(TransactionDTO transactionDTO) {
        Transaction existing = transactionRepository.findById(transactionDTO.getId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        existing.setAmount(transactionDTO.getAmount());
        existing.setTransactionDate(transactionDTO.getTransactionDate());
        existing.setPaymentMode(transactionDTO.getPaymentMode());
        existing.setType(transactionDTO.getType());

        Optional<Category> category = categoryRepository
                .findById(transactionDTO.getCategoryId());
        category.ifPresent(existing::setCategory);

        Currency currency = currencyRepository
                .findByCodeContainingIgnoreCase(transactionDTO.getCurrencyCode());
        existing.setCurrency(currency);

        transactionRepository.save(existing);

        FactTransactionDTO dto =
                mapToFactTransactionDTO(existing, SyncOperation.UPDATE);

        sendToReportingService(List.of(dto));
    }

    @Transactional
    public void updateTransactions(UpdateTransactionDTO transactionDTO) {

        // Collect all IDs across all update groups
        List<String> allIds = transactionDTO.ids();

        // Fetch all transactions in one query
        Map<String, Transaction> transactionMap = transactionRepository
                .findAllById(allIds)
                .stream()
                .collect(Collectors.toMap(Transaction::getId, t -> t));

        List<Transaction> toSave = new ArrayList<>();


        for (String id : transactionDTO.ids()) {
            Transaction existing = transactionMap.get(id);
            if (existing == null) {
                throw new RuntimeException("Transaction not found: " + id);
            }

            if (transactionDTO.type() != null) {
                existing.setType(transactionDTO.type());
            }
            if (transactionDTO.paymentMethod() != null) {
                existing.setPaymentMode(transactionDTO.paymentMethod());
            }
            if (transactionDTO.categoryId() != null) {
                categoryRepository.findById(transactionDTO.categoryId())
                        .ifPresent(existing::setCategory);
            }
            if(transactionDTO.currencyCode() != null){
                existing.setCurrency(currencyRepository.findByCodeContainingIgnoreCase(transactionDTO.currencyCode()));

            }

            toSave.add(existing);
        }

        List<Transaction> saved = transactionRepository.saveAll(toSave);

        List<FactTransactionDTO> factDTOs = saved.stream()
                .map(t -> mapToFactTransactionDTO(t, SyncOperation.UPDATE))
                .toList();
        System.out.println(factDTOs);
        sendToReportingService(factDTOs);
    }

    @Transactional
    public void deleteTransactions(List<String> ids, String userId) {
        if (ids == null || ids.isEmpty()) return;

        transactionRepository.deleteAllByIdAndUserId(ids, userId);

        List<FactTransactionDTO> dtos = ids.stream()
                .map(id -> {
                    FactTransactionDTO dto = new FactTransactionDTO();
                    dto.setId(id);
                    dto.setOperation(SyncOperation.DELETE);
                    return dto;
                })
                .toList();

        sendToReportingService(dtos);
    }

    public FactTransactionDTO mapToFactTransactionDTO(Transaction tx, SyncOperation operation) {
        FactTransactionDTO dto = new FactTransactionDTO();
        dto.setId(tx.getId());
        dto.setAmount(tx.getAmount());
        dto.setCategoryId(tx.getCategory() != null ? tx.getCategory().getId() : null);
        dto.setTransaction_date(tx.getTransactionDate());
        dto.setPaymentMode(tx.getPaymentMode());
        dto.setCurrencyCode(tx.getCurrency() != null ? tx.getCurrency().getCode() : null);
        dto.setOperation(operation);
        dto.setUserId(tx.getUserId());
        dto.setDateKey(tx.getTransactionDate().toLocalDate());
        dto.setType(tx.getType());
        return dto;
    }

    public void sendToReportingService(List<FactTransactionDTO> dtos) {
        System.out.println(reportingServiceApiProperties.getApiBase());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-REPORTING-API-KEY", reportingServiceApiProperties.getApiKey());

        HttpEntity<List<FactTransactionDTO>> request = new HttpEntity<>(dtos,headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                reportingServiceApiProperties.getApiBase() + "/api/transactions/sync",
                request,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(
                    "Failed to sync with Reporting Service: " + response.getBody()
            );
        }
    }


    public Page<TransactionDTO> getCurrentUserTransactions(Pageable pageable, TransactionFilter filter, String userId){
        Page<Transaction> transactions = transactionRepository.findAll(TransactionSpecifications.withFilters(filter,userId), pageable);

        var dtoList = transactions.stream()
                .map(tx -> new TransactionDTO(
                        tx.getId(),
                        tx.getTransactionDate(),
                        tx.getCategory() != null ? tx.getCategory().getId() : "",
                        tx.getDescription(),
                        tx.getAmount(),
                        tx.getCurrency() != null ? tx.getCurrency().getCode() : null, // extract just the code
                        tx.getPaymentMode(),
                        tx.getCreatedAt(),
                        tx.getType(),
                        tx.getCategory() != null ? tx.getCategory().getName() : ""
                ))
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, transactions.getTotalElements());
    }

}
