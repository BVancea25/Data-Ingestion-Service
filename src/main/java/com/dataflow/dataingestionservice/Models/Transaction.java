package com.dataflow.dataingestionservice.Models;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NaturalId;
import org.springframework.batch.item.database.JdbcBatchItemWriter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a financial transaction record.
 * <p>
 * This entity is mapped to the "transactions" table in the database.
 * It contains information such as the user who performed the transaction,
 * the date and time of the transaction, the category, description, amount, currency,
 * payment mode, and the timestamp when the record was created.
 * </p>
 *
 * <p>
 * The entity uses UUID for its primary key generation and for identifying the user.
 * Some fields (like {@code userId} and {@code transactionDate}) are annotated as natural IDs
 * to denote that they have unique business meaning.
 * </p>
 */
@Table(name = "transactions")
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    /**
     * The unique identifier for the transaction.
     */
    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    /**
     * The unique identifier of the user associated with the transaction.
     * <p>
     * Marked as a natural ID and cannot be null.
     * </p>
     */
    @NaturalId
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    /**
     * The date and time when the transaction occurred.
     * <p>
     * Marked as a natural ID and cannot be null.
     * </p>
     */
    @NaturalId
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    /**
     * The category of the transaction.
     * Cannot be null.
     */
    @Column(name = "category", nullable = false)
    private String category;

    /**
     * A description of the transaction.
     * This field is optional.
     */
    @Column(name = "description")
    private String description;

    /**
     * The amount of the transaction.
     * Cannot be null.
     */
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    /**
     * The currency in which the transaction was made.
     */
    @Transient
    private String currencyCode;

    @ManyToOne(targetEntity = Currency.class)
    @JoinColumn(name = "currency_id")
    private Currency currency;
    /**
     * The payment mode used for the transaction (e.g., "Credit Card", "Cash").
     */
    @Column(name = "payment_mode")
    private String paymentMode;

    /**
     * The timestamp when the transaction record was created.
     * Defaults to the current time.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Returns a string representation of the transaction.
     *
     * @return a string containing the transaction details
     */
    @Override
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

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Currency getCurrency() {
        return currency;
    }

    /**
     * Gets the user identifier.
     *
     * @return the {@link String} representing the user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the unique identifier for the transaction.
     *
     * @param id the {@link String} to set as the transaction ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the unique identifier for the transaction.
     *
     * @return the {@link String} representing the transaction ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the transaction ID as a string.
     * Needed for injecting the parameter in the {@link JdbcBatchItemWriter} query
     * @return the transaction ID as a string
     */
    public String getIdAsString() {
        return id.toString();
    }

    /**
     * Returns the user ID as a string.
     * Needed for injecting the parameter in the {@link JdbcBatchItemWriter} query
     * @return the user ID as a string
     */
    public String getUserIdAsString() {
        return userId.toString();
    }

    /**
     * Sets the user identifier.
     *
     * @param userId the {@link String} representing the user ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the transaction date and time.
     *
     * @return the {@link LocalDateTime} of the transaction
     */
    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    /**
     * Sets the transaction date and time.
     *
     * @param transactionDate the {@link LocalDateTime} to set for the transaction
     */
    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    /**
     * Gets the category of the transaction.
     *
     * @return the transaction category as a {@link String}
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category of the transaction.
     *
     * @param category the category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the description of the transaction.
     *
     * @return the transaction description as a {@link String}
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the transaction.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the currency of the transaction.
     *
     * @return the currency as a {@link String}
     */
    public String getCurrencyCode() {
        return currencyCode;
    }

    /**
     * Sets the currency of the transaction.
     *
     * @param currencyCode the currency to set
     */
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    /**
     * Gets the payment mode of the transaction.
     *
     * @return the payment mode as a {@link String}
     */
    public String getPaymentMode() {
        return paymentMode;
    }

    /**
     * Sets the payment mode of the transaction.
     *
     * @param paymentMode the payment mode to set
     */
    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    /**
     * Gets the creation timestamp of the transaction record.
     *
     * @return the {@link LocalDateTime} when the transaction was created
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp of the transaction record.
     *
     * @param createdAt the {@link LocalDateTime} to set as the creation time
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the transaction amount.
     *
     * @return the transaction amount as a {@link BigDecimal}
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the transaction amount.
     *
     * @param amount the {@link BigDecimal} amount to set for the transaction
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
