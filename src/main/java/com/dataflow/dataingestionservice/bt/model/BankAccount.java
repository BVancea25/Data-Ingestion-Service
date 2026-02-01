package com.dataflow.dataingestionservice.bt.model;

import com.dataflow.dataingestionservice.Models.Currency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "bank_accounts")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "iban", unique = true)
    private String iban;

    @ManyToOne
    @JoinColumn(name = "consent_id")
    private UserBtDetail userBtDetail;

    @OneToOne
    @JoinColumn(name = "currency_id")
    private Currency currency;

    @Column(name = "name")
    private String name;

    @Column(name = "last_sync_date")
    private LocalDateTime lastSyncDate;

    @Column(name = "resource_id")
    private String resourceId;

}
