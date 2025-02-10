package com.dataflow.dataingestionservice.Config;

import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Repositories.TransactionRepository;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TransactionItemWriter extends RepositoryItemWriter<Transaction> {
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionItemWriter(final TransactionRepository transactionRepository) {
        super();
        this.transactionRepository = transactionRepository;
    }


}
