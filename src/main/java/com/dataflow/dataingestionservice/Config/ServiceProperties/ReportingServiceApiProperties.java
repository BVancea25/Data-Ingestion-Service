package com.dataflow.dataingestionservice.Config.ServiceProperties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "reporting.service")
@Data
public class ReportingServiceApiProperties {
        private String apiBase;
        private String apiKey;
}
