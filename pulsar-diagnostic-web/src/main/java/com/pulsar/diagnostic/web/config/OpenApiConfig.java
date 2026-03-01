package com.pulsar.diagnostic.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for API documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI pulsarDiagnosticOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pulsar Diagnostic AI Agent API")
                        .description("AI-powered Apache Pulsar diagnostic and troubleshooting system. " +
                                "This API provides endpoints for cluster diagnostics, health inspections, " +
                                "and conversational AI assistance for Pulsar operations.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Pulsar Diagnostic Team")
                                .url("https://github.com/pulsar-diagnostic"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")
                ));
    }
}