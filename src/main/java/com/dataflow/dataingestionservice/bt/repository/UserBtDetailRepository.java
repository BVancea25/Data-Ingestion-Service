package com.dataflow.dataingestionservice.bt.repository;

import com.dataflow.dataingestionservice.bt.model.UserBtDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBtDetailRepository extends JpaRepository<UserBtDetail, String> {

    UserBtDetail findTopByUserIdOrderByCreatedAtDesc(String userId);

    UserBtDetail findUserBtDetailByState(String state);

    UserBtDetail findUserBtDetailByConsentStatusAndUserId(String state, String userId);
}
