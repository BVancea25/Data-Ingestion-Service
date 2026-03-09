package com.dataflow.dataingestionservice.Config;

import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CustomRestTemplateConfiguration {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, SslBundles sslBundles) {
        sslBundles.getBundleNames().forEach(name -> System.out.println("Available SSL Bundle: " + name));
        return builder
                .sslBundle(sslBundles.getBundle("trust"))
                .build();
    }
}
