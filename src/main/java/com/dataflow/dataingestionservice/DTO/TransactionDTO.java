package com.dataflow.dataingestionservice.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionDTO {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    private LocalDateTime transactionDate;
    private String category;
    private String description;
    private BigDecimal amount;
    private String currencyCode;
    private String paymentMode;
    private LocalDateTime createdAt;

    // constructor
    public TransactionDTO(String id, LocalDateTime transactionDate, String category, String description,
                          BigDecimal amount, String currencyCode, String paymentMode, LocalDateTime createdAt) {
        this.id = id;
        this.transactionDate = transactionDate;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.paymentMode = paymentMode;
        this.createdAt = createdAt;
    }

    // getters and setters
}
