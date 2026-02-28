package com.pulsar.diagnostic.web.controller;

import com.pulsar.diagnostic.common.enums.HealthStatus;
import com.pulsar.diagnostic.core.health.HealthCheckService;
import com.pulsar.diagnostic.core.metrics.PrometheusMetricsCollector;
import com.pulsar.diagnostic.knowledge.KnowledgeBaseService;
import com.pulsar.diagnostic.web.dto.SystemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * REST controller for system status and health
 */
@RestController
@RequestMapping("/api")
public class StatusController {

    private static final Logger log = LoggerFactory.getLogger(StatusController.class);

    private static final String VERSION = "1.0.0";
    private static final long START_TIME = System.currentTimeMillis();

    private final HealthCheckService healthCheckService;
    private final PrometheusMetricsCollector metricsCollector;
    private final KnowledgeBaseService knowledgeBaseService;

    public StatusController(HealthCheckService healthCheckService,
                            PrometheusMetricsCollector metricsCollector,
                            KnowledgeBaseService knowledgeBaseService) {
        this.healthCheckService = healthCheckService;
        this.metricsCollector = metricsCollector;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * Get system status
     */
    @GetMapping("/status")
    public SystemStatus getStatus() {
        log.debug("Getting system status");

        HealthStatus clusterHealth;
        try {
            clusterHealth = healthCheckService.isClusterHealthy() ?
                    HealthStatus.HEALTHY : HealthStatus.WARNING;
        } catch (Exception e) {
            clusterHealth = HealthStatus.UNKNOWN;
        }

        boolean prometheusAvailable;
        try {
            prometheusAvailable = metricsCollector.isAvailable();
        } catch (Exception e) {
            prometheusAvailable = false;
        }

        boolean knowledgeBaseReady = knowledgeBaseService.isReady();

        long uptime = System.currentTimeMillis() - START_TIME;

        return SystemStatus.of(VERSION, clusterHealth, prometheusAvailable,
                knowledgeBaseReady, uptime);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public HealthStatus health() {
        return healthCheckService.isClusterHealthy() ?
                HealthStatus.HEALTHY : HealthStatus.WARNING;
    }

    /**
     * Get cluster health details
     */
    @GetMapping("/health/details")
    public Object healthDetails() {
        return healthCheckService.performHealthCheck();
    }

    /**
     * Get knowledge base status
     */
    @GetMapping("/knowledge/status")
    public Object knowledgeStatus() {
        return knowledgeBaseService.getStats();
    }
}