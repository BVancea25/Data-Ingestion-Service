package com.dataflow.dataingestionservice.DTO;

import com.dataflow.dataingestionservice.Utils.Constants.PaymentMethod;
import com.dataflow.dataingestionservice.Utils.Constants.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDTO {
    private String id;
    private LocalDateTime transactionDate;
    private String categoryId;
    private String description;
    private BigDecimal amount;
    private String currencyCode;
    private PaymentMethod paymentMode;
    private LocalDateTime createdAt;
    private TransactionType type;
    private String categoryName;
}
