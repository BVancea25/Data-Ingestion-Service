package com.dataflow.dataingestionservice.bt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

@Table(name = "bt_details")
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class UserBtDetail {

    @Id
    @Column(nullable = false)
    private String consentId;

    @Column
    private String consentStatus;

    @Column
    private String code;

    @Column(length = 4000)
    private String accessToken;

    @Column(length = 4000)
    private String refreshToken;

    @Column
    private String state;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public String getConsentStatus() {
        return consentStatus;
    }

    public void setConsentStatus(String consentStatus) {
        this.consentStatus = consentStatus;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getValidUntill() {
        return validUntill;
    }

    public void setValidUntill(LocalDate validUntill) {
        this.validUntill = validUntill;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }

    @Column
    private String userId;

    @Column
    private Date createdAt;

    @Column
    private Date modifiedAt;

    @Column
    private LocalDate validUntill;

    @Column
    private String codeVerifier;

    @PrePersist
    protected void onCreate(){
        createdAt = new Date();
        modifiedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate(){
        modifiedAt = new Date();
    }

}
