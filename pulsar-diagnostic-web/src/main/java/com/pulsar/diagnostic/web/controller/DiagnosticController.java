package com.pulsar.diagnostic.web.controller;

import com.pulsar.diagnostic.agent.agent.PulsarDiagnosticAgent;
import com.pulsar.diagnostic.web.dto.DiagnosticRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for diagnostic operations
 */
@RestController
@RequestMapping("/api/diagnose")
@Tag(name = "Diagnostics", description = "Endpoints for Pulsar cluster diagnostics and troubleshooting")
public class DiagnosticController {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticController.class);

    private final PulsarDiagnosticAgent agent;

    public DiagnosticController(PulsarDiagnosticAgent agent) {
        this.agent = agent;
    }

    /**
     * Diagnose an issue
     */
    @PostMapping
    @Operation(summary = "Diagnose a Pulsar issue", description = "Submit an issue description, symptoms, or component details for AI-powered diagnosis")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Diagnostic analysis completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public String diagnose(@Valid @RequestBody DiagnosticRequest request) {
        log.info("Received diagnostic request: {}", request.issue());

        if (request.symptoms() != null && !request.symptoms().isEmpty()) {
            return agent.analyzeSymptoms(request.symptoms());
        }

        if (request.componentType() != null && request.componentId() != null) {
            return agent.getRecommendations(request.componentType(), request.componentId());
        }

        return agent.diagnose(request.issue());
    }

    /**
     * Diagnose a specific topic's backlog
     */
    @GetMapping("/topic/{topicName}/backlog")
    @Operation(summary = "Diagnose topic backlog", description = "Analyze backlog issues for a specific topic")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Backlog analysis completed")
    })
    public String diagnoseTopicBacklog(
            @Parameter(description = "Full topic name (e.g., persistent://tenant/namespace/topic)")
            @PathVariable String topicName) {
        log.info("Diagnosing backlog for topic: {}", topicName);
        return agent.diagnose("Analyze the backlog for topic: " + topicName +
                ". Check if there are any issues with consumers or message processing.");
    }

    /**
     * Diagnose connection issues
     */
    @GetMapping("/connections")
    @Operation(summary = "Diagnose connection issues", description = "Analyze cluster connection health and identify issues")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connection diagnosis completed")
    })
    public String diagnoseConnections() {
        log.info("Diagnosing connection issues");
        return agent.diagnose("Diagnose any connection issues in the cluster. " +
                "Check broker connections, client connections, and network health.");
    }

    /**
     * Diagnose performance issues
     */
    @GetMapping("/performance")
    @Operation(summary = "Diagnose performance issues", description = "Analyze cluster performance and identify bottlenecks")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Performance diagnosis completed")
    })
    public String diagnosePerformance() {
        log.info("Diagnosing performance issues");
        return agent.diagnose("Analyze the cluster performance. Check for any bottlenecks, " +
                "slow consumers, or resource constraints.");
    }
}