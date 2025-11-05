package com.dataflow.dataingestionservice.Services;

import com.dataflow.dataingestionservice.Config.TransactionBatchConfig;
import com.dataflow.dataingestionservice.DTO.CurrencyDTO;
import com.dataflow.dataingestionservice.DTO.TransactionDTO;
import com.dataflow.dataingestionservice.DTO.TransactionFilter;
import com.dataflow.dataingestionservice.Models.Currency;
import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Repositories.CurrencyRepository;
import com.dataflow.dataingestionservice.Repositories.TransactionRepository;
import com.dataflow.dataingestionservice.Specifications.TransactionSpecifications;
import com.dataflow.dataingestionservice.Utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    private static final Logger logger = LoggerFactory.getLogger(TransactionBatchConfig.class);
    public void saveTransactions(List<Transaction> transactions){
        for (Transaction transaction : transactions){
            transaction.setId(UUID.randomUUID().toString());
            transaction.setUserId(SecurityUtils.getCurrentUserUuid());
            Currency currency = currencyRepository.findByCodeContainingIgnoreCase(transaction.getCurrencyCode());

            if(currency != null){
                transaction.setCurrency(currency);
            }else{
                logger.error("Couldn't find currency for code "+transaction.getCurrencyCode());
            }
        }
        transactionRepository.saveAll(transactions);
    }

    public Page<TransactionDTO> getCurrentUserTransactions(Pageable pageable, TransactionFilter filter, String userId){
        Page<Transaction> transactions = transactionRepository.findAll(TransactionSpecifications.withFilters(filter,userId), pageable);

        var dtoList = transactions.stream()
                .map(tx -> new TransactionDTO(
                        tx.getId(),
                        tx.getTransactionDate(),
                        tx.getCategory(),
                        tx.getDescription(),
                        tx.getAmount(),
                        tx.getCurrency() != null ? tx.getCurrency().getCode() : null, // extract just the code
                        tx.getPaymentMode(),
                        tx.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, transactions.getTotalElements());
    }


    @Transactional
    public void deleteTransactions(List<String> ids, String userId){
        if(ids == null || ids.isEmpty()) return;
        transactionRepository.deleteAllByIdAndUserId(ids,userId);
    }

    public void updateTransaction(TransactionDTO transactionDTO){
        Optional<Transaction> transaction = transactionRepository.findById(transactionDTO.getId());
        if(transaction.isPresent()) return;
        Transaction modifiedTransaction = new Transaction();
        Currency currency = currencyRepository.findByCodeContainingIgnoreCase(transactionDTO.getCurrencyCode());
        modifiedTransaction.setCategory(transactionDTO.getCategory());
        modifiedTransaction.setCurrency(currency);
        modifiedTransaction.setAmount(transactionDTO.getAmount());
        modifiedTransaction.setDescription(transactionDTO.getDescription());
        modifiedTransaction.setPaymentMode(transactionDTO.getPaymentMode());
        transactionRepository.save(modifiedTransaction);

    }
}
