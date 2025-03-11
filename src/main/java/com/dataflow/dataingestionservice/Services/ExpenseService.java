package com.dataflow.dataingestionservice.Services;

import com.dataflow.dataingestionservice.Config.TransactionBatchConfig;
import com.dataflow.dataingestionservice.Models.Currency;
import com.dataflow.dataingestionservice.Models.Expense;
import com.dataflow.dataingestionservice.Repositories.CurrencyRepository;
import com.dataflow.dataingestionservice.Repositories.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CurrencyRepository currencyRepository;
    private static final Logger logger = LoggerFactory.getLogger(TransactionBatchConfig.class);
    public String saveExpenses(List<Expense> expenseList){
        String response="";
        try{
            for(Expense expense:expenseList){

                expense.setId(UUID.randomUUID());
                Optional<Currency> optionalCurrency = currencyRepository.findById(UUID.fromString(expense.getCurrency_id()));

                if (optionalCurrency.isPresent()) {
                    Currency currency = optionalCurrency.get();
                    expense.setCurrency(currency);
                }
            }
            expenseRepository.saveAll(expenseList);
            response="Expenses saved!";
        }catch (Exception e){
            logger.error("Expense service, saveAll",e);
            response="Error saving expenses";
        }

        return response;
    }
}
