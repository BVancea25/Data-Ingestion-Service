package com.dataflow.dataingestionservice.Utils;

import com.dataflow.dataingestionservice.Config.ServiceProperties.ReportingServiceApiProperties;
import com.dataflow.dataingestionservice.DTO.FactTransactionDTO;
import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Utils.Constants.SyncOperation;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@StepScope
public class ReportingSyncItemWriteListener implements ItemWriteListener<Transaction> {
    private final RestTemplate restTemplate;
    private final ReportingServiceApiProperties props;

    public ReportingSyncItemWriteListener(
            ReportingServiceApiProperties props) {
        this.restTemplate = new RestTemplate();
        this.props = props;
    }

    @Override
    public void afterWrite(Chunk<? extends Transaction> chunk) {
        if (chunk.isEmpty()) return;

        List<Transaction> transactions = (List<Transaction>) chunk.getItems();

        List<FactTransactionDTO> dtos = transactions.stream()
                .map(tx -> {
                    FactTransactionDTO dto = new FactTransactionDTO();
                    dto.setId(tx.getId());
                    dto.setAmount(tx.getAmount());
                    dto.setCategory(tx.getCategory());
                    dto.setTransaction_date(tx.getTransactionDate());
                    dto.setPaymentMode(tx.getPaymentMode());
                    dto.setCurrencyCode(tx.getCurrency().getCode());
                    dto.setOperation(SyncOperation.CREATE);
                    dto.setUserId(tx.getUserId());
                    dto.setDateKey(tx.getTransactionDate().toLocalDate());
                    dto.setType(tx.getType());
                    return dto;
                })
                .toList();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-REPORTING-API-KEY", props.getApiKey());

        HttpEntity<List<FactTransactionDTO>> request = new HttpEntity<>(dtos,headers);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                props.getApiBase() + "/api/transactions/sync",
                request,
                Void.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to sync batch to reporting service");
        }
    }

}
