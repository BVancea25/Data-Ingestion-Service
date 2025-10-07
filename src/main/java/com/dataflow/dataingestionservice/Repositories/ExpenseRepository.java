package com.dataflow.dataingestionservice.Repositories;

import com.dataflow.dataingestionservice.Models.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String> {
}
