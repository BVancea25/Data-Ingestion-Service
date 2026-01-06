package com.dataflow.dataingestionservice.bt.repository;

import com.dataflow.dataingestionservice.bt.model.BankAccount;
import com.dataflow.dataingestionservice.bt.model.UserBtDetail;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccount, Integer> {
    BankAccount getBankAccountByResourceId(String resourceId);
    List<BankAccount> getBankAccountsByUserBtDetail(UserBtDetail userBtDetail);
    @Modifying
    @Transactional
    @Query("UPDATE BankAccount b SET b.lastSyncDate = :lastSyncDate WHERE b.id = :id")
    int updateBankAccountById(@Param("id") Integer id, @Param("lastSyncDate") LocalDateTime lastSyncDate);
}
