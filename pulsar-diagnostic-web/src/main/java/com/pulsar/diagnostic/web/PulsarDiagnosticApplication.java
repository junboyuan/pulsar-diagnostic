package com.pulsar.diagnostic.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Pulsar Diagnostic Agent
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
    "com.pulsar.diagnostic.web",
    "com.pulsar.diagnostic.agent",
    "com.pulsar.diagnostic.knowledge",
    "com.pulsar.diagnostic.core"
})
public class PulsarDiagnosticApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulsarDiagnosticApplication.class, args);
    }
}