package com.dataflow.dataingestionservice.bt.model;

import com.dataflow.dataingestionservice.Models.Currency;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "bank_accounts")
@Entity
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

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDateTime getLastSyncDate() {
        return lastSyncDate;
    }

    public void setLastSyncDate(LocalDateTime lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public UserBtDetail getUserBtDetail() {
        return userBtDetail;
    }

    public void setUserBtDetail(UserBtDetail userBtDetail) {
        this.userBtDetail = userBtDetail;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
