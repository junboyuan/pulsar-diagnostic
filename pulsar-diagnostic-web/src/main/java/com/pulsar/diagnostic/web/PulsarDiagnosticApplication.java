package com.pulsar.diagnostic.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Pulsar Diagnostic Agent
 */
@SpringBootApplication
@EnableScheduling
public class PulsarDiagnosticApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulsarDiagnosticApplication.class, args);
    }
}