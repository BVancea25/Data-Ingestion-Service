package com.dataflow.dataingestionservice.bt.controller;

import com.dataflow.dataingestionservice.bt.service.BtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CallBackController {
    private static final Logger logger = LoggerFactory.getLogger(CallBackController.class);
    private final BtService btService;

    public CallBackController(BtService btService){
        this.btService = btService;
    }

    @GetMapping("/bt/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String session_state
    ) {
        try {
            // Exchange code for tokens
            btService.exchangeCodeForTokens(code, state);

            // Optional: Redirect to frontend success page
             return ResponseEntity.status(HttpStatus.FOUND)
                 .header(HttpHeaders.LOCATION, "http://localhost:9527/import")
                 .build();

            //return ResponseEntity.ok("Consent successful. You can close this page.");
        } catch (Exception e) {
            logger.error("Error processing BT consent callback", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Unable to complete bank consent. Please try connecting again.");
        }
    }
}
