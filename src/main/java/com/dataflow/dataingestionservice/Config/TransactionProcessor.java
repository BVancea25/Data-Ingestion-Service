package com.dataflow.dataingestionservice.Config;

import com.dataflow.dataingestionservice.Models.Transaction;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.UUID;

/**
 * A Spring Batch {@code ItemProcessor} implementation for processing {@link Transaction} items.
 * <p>
 * This processor assigns a new random UUID to the {@code id} field of each {@code Transaction} before it is written.
 * This ensures that each transaction record has a unique identifier when persisted.
 * </p>
 *
 * <p>
 * The class uses Lombok's {@code @AllArgsConstructor} and {@code @NoArgsConstructor} annotations to generate both
 * an all-arguments constructor and a no-arguments constructor.
 * </p>
 *
 * @see org.springframework.batch.item.ItemProcessor
 */
@AllArgsConstructor
@NoArgsConstructor
public class TransactionProcessor implements ItemProcessor<Transaction, Transaction> {

    /**
     * Processes a {@link Transaction} by assigning it a new random UUID.
     *
     * @param item the {@link Transaction} item to process
     * @return the processed {@link Transaction} with a new UUID assigned to its {@code id} field
     * @throws Exception if an error occurs during processing
     */
    @Override
    public Transaction process(Transaction item) throws Exception {
        item.setId(UUID.randomUUID());
        return item;
    }
}
