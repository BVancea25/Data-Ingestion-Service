package com.dataflow.dataingestionservice.bt.controller;

import com.dataflow.dataingestionservice.bt.service.BtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BtController {
    private final BtService btService;

    public BtController(BtService btService){
        this.btService = btService;
    }

    @GetMapping("/bt/consent/create")
    public String createConsentAndRedirect(){
        return btService.createConsentAndBuildRedirect();
    }

}
