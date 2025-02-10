package com.dataflow.dataingestionservice.Repositories;

import com.dataflow.dataingestionservice.Models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction,Long>{
}
