package com.pulsar.diagnostic.web.controller;

import com.pulsar.diagnostic.agent.agent.PulsarDiagnosticAgent;
import com.pulsar.diagnostic.web.dto.DiagnosticRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for diagnostic operations
 */
@RestController
@RequestMapping("/api/diagnose")
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
    public String diagnose(@RequestBody DiagnosticRequest request) {
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
    public String diagnoseTopicBacklog(@PathVariable String topicName) {
        log.info("Diagnosing backlog for topic: {}", topicName);
        return agent.diagnose("Analyze the backlog for topic: " + topicName +
                ". Check if there are any issues with consumers or message processing.");
    }

    /**
     * Diagnose connection issues
     */
    @GetMapping("/connections")
    public String diagnoseConnections() {
        log.info("Diagnosing connection issues");
        return agent.diagnose("Diagnose any connection issues in the cluster. " +
                "Check broker connections, client connections, and network health.");
    }

    /**
     * Diagnose performance issues
     */
    @GetMapping("/performance")
    public String diagnosePerformance() {
        log.info("Diagnosing performance issues");
        return agent.diagnose("Analyze the cluster performance. Check for any bottlenecks, " +
                "slow consumers, or resource constraints.");
    }
}