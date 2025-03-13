package com.dataflow.dataingestionservice.Services;

import com.dataflow.dataingestionservice.Config.TransactionBatchConfig;
import com.dataflow.dataingestionservice.Models.Currency;
import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Repositories.CurrencyRepository;
import com.dataflow.dataingestionservice.Repositories.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    private static final Logger logger = LoggerFactory.getLogger(TransactionBatchConfig.class);
    public void saveTransactions(List<Transaction> transactions){
        for (Transaction transaction : transactions){
            transaction.setId(UUID.randomUUID());
            Currency currency = currencyRepository.findByCode(transaction.getCurrencyCode());

            if(currency != null){
                transaction.setCurrency(currency);
            }else{
                logger.error("Couldn't find currency for code "+transaction.getCurrencyCode());
            }
        }
        transactionRepository.saveAll(transactions);
    }
}
