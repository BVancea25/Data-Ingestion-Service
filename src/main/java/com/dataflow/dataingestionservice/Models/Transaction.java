package com.dataflow.dataingestionservice.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NaturalId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "transactions")
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NaturalId
    @Column(name = "user_id" ,nullable = false)
    private UUID userId;

    @NaturalId
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "description")
    private String description;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency = "USD";

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public String toString() {
        return "Transaction{" +
                "userId=" + userId.toString() +
                ", transactionDate=" + transactionDate.toString() +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", amount=" + amount.toString() +
                ", currency='" + currency + '\'' +
                ", paymentMode='" + paymentMode + '\'' +
                '}';
    }
    public UUID getUserId() {
        return userId;
    }

    public void setId(UUID id){
        this.id=id;
    }
    public UUID getId(){
        return id;
    }

    public String getIdAsString() {
        return id.toString();
    }
    public String getUserIdAsString() {
        return userId.toString();
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount){
        this.amount=amount;
    }
}
