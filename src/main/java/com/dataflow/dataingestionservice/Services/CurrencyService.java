package com.dataflow.dataingestionservice.Services;

import com.dataflow.dataingestionservice.DTO.CurrencyDTO;
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

    public Optional<Currency> getCurrencyById(String id){
        return currencyRepository.findById(id);
    }

    public Currency getCurrencyByCode(String code){
        return currencyRepository.findByCodeContainingIgnoreCase(code);
    }

    public List<CurrencyDTO> searchCurrencies(String search){
       return currencyRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(search,search)
                .stream()
                .map(c -> new CurrencyDTO(c.getCode(),c.getName()))
                .toList();
    }
}
