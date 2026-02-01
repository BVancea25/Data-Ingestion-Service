package com.dataflow.dataingestionservice.Specifications;

import com.dataflow.dataingestionservice.DTO.TransactionFilter;
import com.dataflow.dataingestionservice.Models.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TransactionSpecifications {

    public static Specification<Transaction> withFilters(TransactionFilter filter, String userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only user's transactions
            predicates.add(cb.equal(root.get("userId"), userId));

            if (filter.getCategory() != null && !filter.getCategory().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), filter.getCategory().toLowerCase()));
            }

            if (filter.getCurrencyCode() != null && !filter.getCurrencyCode().isEmpty()) {
                predicates.add(cb.equal(root.get("currencyCode"), filter.getCurrencyCode()));
            }

            if (filter.getPaymentMode() != null && !filter.getPaymentMode().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("paymentMode")), filter.getPaymentMode().toLowerCase()));
            }

            if (filter.getDescription() != null && !filter.getDescription().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + filter.getDescription().toLowerCase() + "%"));
            }

            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), filter.getStartDate()));
            }

            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), filter.getEndDate()));
            }

            if(filter.getType() != null){
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }
            // Combine all with AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
