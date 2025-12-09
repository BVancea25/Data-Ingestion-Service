package com.dataflow.dataingestionservice.Repositories;

import com.dataflow.dataingestionservice.Models.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,String>, JpaSpecificationExecutor<Transaction> {
    @Modifying
    @Query("DELETE FROM Transaction t WHERE t.id IN :ids AND t.userId = :userId")
    void deleteAllByIdAndUserId(@Param("ids") List<String> ids, @Param("userId") String userId);

    @Query("SELECT t.btTransactionId FROM Transaction t WHERE t.btTransactionId IN :btIds AND t.userId = :userId")
    List<String> getTransactionsIdsByBtTransactionId(List<String> btIds, String userId);
}
