package com.dataflow.dataingestionservice.bt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.Date;

@Table(name = "bt_details")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
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
