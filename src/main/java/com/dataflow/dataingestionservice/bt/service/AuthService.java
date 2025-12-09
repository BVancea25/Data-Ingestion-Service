package com.dataflow.dataingestionservice.bt.service;

import com.dataflow.dataingestionservice.bt.config.BtApiProperties;
import com.dataflow.dataingestionservice.bt.model.UserBtDetail;
import com.dataflow.dataingestionservice.bt.repository.UserBtDetailRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AuthService {
    private final BtApiProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    private final UserBtDetailRepository userBtDetailRepository;

    public AuthService(BtApiProperties props, UserBtDetailRepository userBtDetailRepository){
        this.props = props;
        this.userBtDetailRepository = userBtDetailRepository;
    }
    public boolean refreshAccessToken(UserBtDetail userBtDetail) {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", props.getClientId());
            body.add("client_secret", props.getClientSecret());
            body.add("refresh_token", userBtDetail.getRefreshToken());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    props.getTokenUrl(), HttpMethod.POST, request, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> bodyMap = response.getBody();

                String newAccessToken = (String) bodyMap.get("access_token");
                String newRefreshToken = (String) bodyMap.get("refresh_token");

                userBtDetail.setAccessToken(newAccessToken);
                if (newRefreshToken != null) userBtDetail.setRefreshToken(newRefreshToken);

                userBtDetailRepository.save(userBtDetail);

                System.out.println("Refreshed tokens successfully.");
                return true;
            } else {
                System.out.println("Token refresh failed: " + response.getBody());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error refreshing access token: " + e.getMessage());
            return false;
        }
    }
}
