package com.dataflow.dataingestionservice.Controllers;

import com.dataflow.dataingestionservice.Models.Expense;
import com.dataflow.dataingestionservice.Services.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expense")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;
    @PostMapping
    public String saveExpenses(@RequestBody List<Expense> expenseList){
        return expenseService.saveExpenses(expenseList);
    }
}
