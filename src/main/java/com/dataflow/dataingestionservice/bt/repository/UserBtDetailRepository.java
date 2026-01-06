package com.dataflow.dataingestionservice.bt.repository;

import com.dataflow.dataingestionservice.bt.model.UserBtDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBtDetailRepository extends JpaRepository<UserBtDetail, String> {

    UserBtDetail findTopByUserIdOrderByCreatedAtDesc(String userId);

    UserBtDetail findUserBtDetailByState(String state);

    UserBtDetail findUserBtDetailByConsentStatusAndUserId(String state, String userId);

    @Query("SELECT bt FROM UserBtDetail bt WHERE bt.userId = :userId AND bt.consentStatus = 'valid' AND bt.validUntill > CURRENT_DATE ")
    Optional<UserBtDetail> findValidConsent(String userId);
}
