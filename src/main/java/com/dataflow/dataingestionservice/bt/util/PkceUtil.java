package com.dataflow.dataingestionservice.bt.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PkceUtil {
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateCodeVerifier(){
        byte[] code = new byte[32];
        secureRandom.nextBytes(code);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(code);
    }

    public static String generateCodeChallenge(String codeVerifier){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hashed);
        }catch (Exception e){
            throw new RuntimeException("Failed to generate PKCE code challange", e);
        }
    }
}
