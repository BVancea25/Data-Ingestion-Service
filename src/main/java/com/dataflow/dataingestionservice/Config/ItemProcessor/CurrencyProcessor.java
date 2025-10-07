package com.dataflow.dataingestionservice.Config.ItemProcessor;

import com.dataflow.dataingestionservice.Models.Currency;
import com.dataflow.dataingestionservice.Models.Transaction;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class CurrencyProcessor implements ItemProcessor<Currency, Currency> {
    @Override
    public Currency process(Currency item) throws Exception {
        item.setId(UUID.randomUUID().toString());
        return item;
    }
}
