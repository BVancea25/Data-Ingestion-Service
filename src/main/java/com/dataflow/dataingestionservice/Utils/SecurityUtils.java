package com.dataflow.dataingestionservice.Utils;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class SecurityUtils {
    public static String getCurrentUserUuid(){
        Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return jwt.getSubject();
    }
}

