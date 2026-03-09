package com.dataflow.dataingestionservice.DTO;

import com.dataflow.dataingestionservice.Models.Category;
import com.dataflow.dataingestionservice.Utils.Constants.PaymentMethod;
import com.dataflow.dataingestionservice.Utils.Constants.TransactionType;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionFilter {
    private String currencyCode;
    private PaymentMethod paymentMode;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private TransactionType type;
    private String categoryId;

}
