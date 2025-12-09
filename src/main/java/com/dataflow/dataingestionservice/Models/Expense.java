package com.dataflow.dataingestionservice.Models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Table(name = "expenses")
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    @Id
    private String Id;

    @ManyToOne(targetEntity = Currency.class)
    @JoinColumn(name = "currency_id")
    private Currency currency;

    @Column(name = "description")
    private String description;

    @Column(name = "expense_type", nullable = false)
    private String expenseType;

    @Column(name = "due_date", nullable = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "M/d/yyyy")
    private LocalDate dueDate;

    @Column(name = "bt_transaction_id", nullable = true)
    private String btTransactionId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Getter
    @Transient
    private String currency_id;

    public String getBtTransactionId() {
        return btTransactionId;
    }

    public void setBtTransactionId(String btTransactionId) {
        this.btTransactionId = btTransactionId;
    }

    public String getCurrencyId(){
        return this.currency_id;
    }
    public String getId() {
        return Id;
    }

    public void setId(String id) {
        this.Id = id;
    }


    public String getDescription() {
        return description;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
