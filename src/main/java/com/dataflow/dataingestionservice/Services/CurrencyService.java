package com.dataflow.dataingestionservice.Services;

import com.dataflow.dataingestionservice.Models.Currency;
import com.dataflow.dataingestionservice.Repositories.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CurrencyService {

    @Autowired
    CurrencyRepository currencyRepository;

    public List<Currency> getCurrencies(){
        return currencyRepository.findAll();
    }

    public Optional<Currency> getCurrencyById(UUID id){
        return currencyRepository.findById(id);
    }

    public Currency getCurrencyByCode(String code){
        return currencyRepository.findByCode(code);
    }
}
