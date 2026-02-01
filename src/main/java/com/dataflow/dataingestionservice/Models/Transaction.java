package com.dataflow.dataingestionservice.Models;

import com.dataflow.dataingestionservice.Utils.Constants.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NaturalId;
import org.springframework.batch.item.database.JdbcBatchItemWriter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@Data
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


    @Column(name = "bt_transaction_id", nullable = true)
    private String btTransactionId;

    /**
     * The timestamp when the transaction record was created.
     * Defaults to the current time.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name="type", nullable = false)
    private TransactionType type = TransactionType.INCOME;

    @PrePersist
    public void prePersist() {
        if (type == null) {
            type = TransactionType.INCOME;
        }
    }
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

}
