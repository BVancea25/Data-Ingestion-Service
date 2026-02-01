package com.dataflow.dataingestionservice.DTO;

import com.dataflow.dataingestionservice.Utils.Constants.SyncOperation;
import com.dataflow.dataingestionservice.Utils.Constants.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactTransactionDTO {
    private String id;               // transactionId
    private BigDecimal amount;
    private String category;
    private String currencyCode;
    private LocalDateTime transaction_date;
    private String paymentMode;
    private SyncOperation operation;
    private String userId;
    private LocalDate dateKey;
    private TransactionType type;
}
