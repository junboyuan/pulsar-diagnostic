package com.pulsar.diagnostic.core.metrics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for metrics collection
 */
@Configuration
public class MetricsConfig {

    private final WebClient prometheusWebClient;

    public MetricsConfig(WebClient prometheusWebClient) {
        this.prometheusWebClient = prometheusWebClient;
    }

    @Bean
    public PrometheusMetricsCollector prometheusMetricsCollector() {
        return new PrometheusMetricsCollector(prometheusWebClient);
    }
}