package com.dataflow.dataingestionservice.Repositories;

import com.dataflow.dataingestionservice.Models.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface CurrencyRepository extends JpaRepository<Currency, UUID> {
   Currency findByCode(String code);
}
