package com.dataflow.dataingestionservice.DTO;

import com.dataflow.dataingestionservice.Utils.Constants.PaymentMethod;
import com.dataflow.dataingestionservice.Utils.Constants.TransactionType;

import java.util.List;

public record UpdateTransactionDTO(List<String> ids , TransactionType type, PaymentMethod paymentMethod, String categoryId, String currencyCode) {
}
