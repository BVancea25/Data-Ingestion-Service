package com.dataflow.dataingestionservice.Controllers;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/me")
    public String getCurrentUser(@AuthenticationPrincipal Jwt jwt){
        return "Hey " + jwt.getClaim("preferred_username");
    }
}
