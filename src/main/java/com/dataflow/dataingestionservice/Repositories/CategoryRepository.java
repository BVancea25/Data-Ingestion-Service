package com.dataflow.dataingestionservice.Repositories;

import com.dataflow.dataingestionservice.Models.Category;
import com.dataflow.dataingestionservice.Utils.Constants.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findAllByUserId(String userId);

    List<Category> findAllByUserIdAndType(String userId, TransactionType type);

    boolean existsByUserIdAndNameAndType(String userId, String name, TransactionType type);

    boolean existsByUserIdAndName(String userId, String name);

    Category findByNameIgnoreCase(String name);

    boolean existsByUserIdAndNameAndIdNot(String userId, String name, String id);
}
