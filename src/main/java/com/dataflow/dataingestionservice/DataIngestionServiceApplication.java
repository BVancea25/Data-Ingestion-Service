package com.dataflow.dataingestionservice;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DataIngestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataIngestionServiceApplication.class, args);
    }

}
