package com.pulsar.diagnostic.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Web client configuration for external service calls
 */
@Configuration
public class WebClientConfig {

    private final PulsarConfig pulsarConfig;

    public WebClientConfig(PulsarConfig pulsarConfig) {
        this.pulsarConfig = pulsarConfig;
    }

    /**
     * WebClient bean for Prometheus API calls
     */
    @Bean
    public WebClient prometheusWebClient() {
        return WebClient.builder()
                .baseUrl(pulsarConfig.getPrometheusUrl())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}