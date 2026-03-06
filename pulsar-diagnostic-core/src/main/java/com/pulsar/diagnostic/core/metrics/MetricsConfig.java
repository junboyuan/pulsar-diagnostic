package com.pulsar.diagnostic.core.metrics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for metrics collection
 */
@Configuration
public class MetricsConfig {

    private final RestClient prometheusRestClient;

    public MetricsConfig(RestClient prometheusRestClient) {
        this.prometheusRestClient = prometheusRestClient;
    }

    @Bean
    public PrometheusMetricsCollector prometheusMetricsCollector() {
        return new PrometheusMetricsCollector(prometheusRestClient);
    }
}