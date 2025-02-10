package com.dataflow.dataingestionservice.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.protocol.types.Field;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.MySQLCastingJsonJdbcType;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(
        name = "custom_transaction_fields",
        indexes = @Index(columnList = "transaction_id",name = "transaction_id_index")
)
@AllArgsConstructor
@NoArgsConstructor
public class CustomTransactionField {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne
    @JoinColumn(name = "transaction_id",nullable = false)
    private Transaction transaction;

    @Column(name = "custom_data")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String,Object> data;

}
