package com.dataflow.dataingestionservice.DTO;

import com.dataflow.dataingestionservice.Utils.Constants.TransactionType;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionFilter {
    private String category;
    private String currencyCode;
    private String paymentMode;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private TransactionType type;

}
