package com.dataflow.dataingestionservice.Repositories;

import com.dataflow.dataingestionservice.Models.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String> {
    @Query("SELECT e.btTransactionId FROM Expense e WHERE e.btTransactionId IN :btIds AND e.userId = :userId")
    List<String> getExpensesIdsByBtTransactionId(List<String> btIds, String userId);
}
