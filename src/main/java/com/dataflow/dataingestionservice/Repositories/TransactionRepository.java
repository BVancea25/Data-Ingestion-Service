package com.dataflow.dataingestionservice.Repositories;

import com.dataflow.dataingestionservice.Models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Long>{
}
