package com.dataflow.dataingestionservice.Config;

import com.dataflow.dataingestionservice.Models.Transaction;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import org.springframework.batch.item.ItemProcessor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
public class TransactionProcessor implements ItemProcessor<Transaction,Transaction> {
    @Override
    public Transaction process(Transaction item) throws Exception {
        item.setId(UUID.randomUUID());
        return item;
    }
}
