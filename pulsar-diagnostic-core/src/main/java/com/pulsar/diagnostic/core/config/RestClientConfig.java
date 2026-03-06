package com.pulsar.diagnostic.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Rest client configuration for external service calls
 */
@Configuration
public class RestClientConfig {

    private final PulsarConfig pulsarConfig;

    public RestClientConfig(PulsarConfig pulsarConfig) {
        this.pulsarConfig = pulsarConfig;
    }

    /**
     * RestClient bean for Prometheus API calls
     */
    @Bean
    public RestClient prometheusRestClient() {
        return RestClient.builder()
                .baseUrl(pulsarConfig.getPrometheusUrl())
                .build();
    }
}