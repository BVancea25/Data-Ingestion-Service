package com.dataflow.dataingestionservice.Config.ItemProcessor;

import com.dataflow.dataingestionservice.Models.Currency;
import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Repositories.CurrencyRepository;
import com.dataflow.dataingestionservice.Utils.SecurityUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.batch.item.ItemProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A Spring Batch {@code ItemProcessor} implementation for processing {@link Transaction} items.
 * <p>
 * This processor assigns a new random UUID to the {@code id} field of each {@code Transaction} before it is written.
 * This ensures that each transaction record has a unique identifier when persisted.
 * </p>
 *
 *
 * @see org.springframework.batch.item.ItemProcessor
 */

public class TransactionProcessor implements ItemProcessor<Transaction, Transaction> {

    private final Map<String, Currency> currencyCache = new HashMap<>();
    private final CurrencyRepository currencyRepository;

    public TransactionProcessor(CurrencyRepository currencyRepository){
        this.currencyRepository=currencyRepository;
    }
    /**
     * Processes a {@link Transaction} by assigning it a new random UUID.
     *
     * @param item the {@link Transaction} item to process
     * @return the processed {@link Transaction} with a new UUID assigned to its {@code id} field
     * @throws Exception if an error occurs during processing
     */
    @Override
    public Transaction process(Transaction item) throws Exception {
        String currencyCode = item.getCurrencyCode();
        Currency currency;
        currency=currencyCache.get(currencyCode);

        if(currency == null) {
            currency = currencyRepository.findByCodeContainingIgnoreCase(currencyCode);
        }
        if(currency  == null){
            throw new IllegalStateException("Currency not found for code: " + currencyCode);
        }
        item.setCurrency(currency);
        item.setId(UUID.randomUUID().toString());
        item.setUserId(SecurityUtils.getCurrentUserUuid());
        return item;
    }

    @PostConstruct
    public void init(){
        for (Currency currency : currencyRepository.findAll()) {
            currencyCache.put(currency.getCode(), currency);
        }
    }
}
