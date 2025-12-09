package com.dataflow.dataingestionservice.bt.dto;

public class BankAccountDTO {
    private String currency;
    private String iban;
    private String name;

    private String resourceId;
    public BankAccountDTO(String currency, String iban, String name, String resourceId) {
        this.currency = currency;
        this.iban = iban;
        this.name = name;
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
