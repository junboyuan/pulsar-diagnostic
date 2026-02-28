package com.pulsar.diagnostic.core.config;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for Pulsar Admin client
 */
@Configuration
public class PulsarAdminConfig {

    private static final Logger log = LoggerFactory.getLogger(PulsarAdminConfig.class);

    private final PulsarConfig pulsarConfig;

    public PulsarAdminConfig(PulsarConfig pulsarConfig) {
        this.pulsarConfig = pulsarConfig;
    }

    /**
     * Create PulsarAdmin bean
     */
    @Bean(destroyMethod = "close")
    public PulsarAdmin pulsarAdmin() throws PulsarClientException {
        log.info("Creating PulsarAdmin client for URL: {}", pulsarConfig.getAdminUrl());

        PulsarAdminBuilder builder = PulsarAdmin.builder()
                .serviceHttpUrl(pulsarConfig.getAdminUrl())
                .connectionTimeout(pulsarConfig.getConnectionTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(pulsarConfig.getRequestTimeoutSeconds(), TimeUnit.SECONDS);

        // Configure authentication if token is provided
        if (pulsarConfig.getAuthToken() != null && !pulsarConfig.getAuthToken().isEmpty()) {
            log.info("Configuring token authentication");
            builder.authentication(AuthenticationFactory.token(pulsarConfig.getAuthToken()));
        }

        // Configure TLS if enabled
        if (pulsarConfig.isTlsEnabled()) {
            log.info("TLS is enabled");
            if (pulsarConfig.getTlsCertificatePath() != null) {
                builder.tlsTrustCertsFilePath(pulsarConfig.getTlsCertificatePath());
            }
            builder.allowTlsInsecureConnection(false);
        }

        return builder.build();
    }
}