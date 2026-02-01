package com.dataflow.dataingestionservice.Services;

import com.dataflow.dataingestionservice.Config.ServiceProperties.ReportingServiceApiProperties;
import com.dataflow.dataingestionservice.DTO.FactTransactionDTO;
import com.dataflow.dataingestionservice.DTO.TransactionDTO;
import com.dataflow.dataingestionservice.DTO.TransactionFilter;
import com.dataflow.dataingestionservice.Models.Currency;
import com.dataflow.dataingestionservice.Models.Transaction;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CurrencyRepository currencyRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private final ReportingServiceApiProperties reportingServiceApiProperties;
    public TransactionService(TransactionRepository transactionRepository,
                              CurrencyRepository currencyRepository, ReportingServiceApiProperties reportingServiceApiProperties) {
        this.transactionRepository = transactionRepository;
        this.currencyRepository = currencyRepository;
        this.reportingServiceApiProperties = reportingServiceApiProperties;
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
                    tx.setCategory(dto.getCategory());
                    tx.setTransactionDate(dto.getTransactionDate());
                    tx.setPaymentMode(dto.getPaymentMode());
                    tx.setDescription(dto.getDescription());
                    tx.setType(dto.getType());

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
        existing.setCategory(transactionDTO.getCategory());
        existing.setTransactionDate(transactionDTO.getTransactionDate());
        existing.setPaymentMode(transactionDTO.getPaymentMode());
        existing.setType(transactionDTO.getType());

        Currency currency = currencyRepository
                .findByCodeContainingIgnoreCase(transactionDTO.getCurrencyCode());
        existing.setCurrency(currency);

        transactionRepository.save(existing);

        FactTransactionDTO dto =
                mapToFactTransactionDTO(existing, SyncOperation.UPDATE);

        sendToReportingService(List.of(dto));
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

    private FactTransactionDTO mapToFactTransactionDTO(Transaction tx, SyncOperation operation) {
        FactTransactionDTO dto = new FactTransactionDTO();
        dto.setId(tx.getId());
        dto.setAmount(tx.getAmount());
        dto.setCategory(tx.getCategory());
        dto.setTransaction_date(tx.getTransactionDate());
        dto.setPaymentMode(tx.getPaymentMode());
        dto.setCurrencyCode(tx.getCurrency() != null ? tx.getCurrency().getCode() : null);
        dto.setOperation(operation);
        dto.setUserId(tx.getUserId());
        dto.setDateKey(tx.getTransactionDate().toLocalDate());
        dto.setType(tx.getType());
        return dto;
    }

    private void sendToReportingService(List<FactTransactionDTO> dtos) {
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
                        tx.getCategory(),
                        tx.getDescription(),
                        tx.getAmount(),
                        tx.getCurrency() != null ? tx.getCurrency().getCode() : null, // extract just the code
                        tx.getPaymentMode(),
                        tx.getCreatedAt(),
                        tx.getType()
                ))
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, transactions.getTotalElements());
    }

}
