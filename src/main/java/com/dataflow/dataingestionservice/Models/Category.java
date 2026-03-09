package com.dataflow.dataingestionservice.Models;

import com.dataflow.dataingestionservice.Utils.Constants.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "categories",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "name"})
    }
)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Category {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    private String color;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();


}
