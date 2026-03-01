package com.pulsar.diagnostic.web.controller;

import com.pulsar.diagnostic.agent.agent.PulsarDiagnosticAgent;
import com.pulsar.diagnostic.web.dto.InspectionRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for inspection operations
 */
@RestController
@RequestMapping("/api/inspect")
public class InspectionController {

    private static final Logger log = LoggerFactory.getLogger(InspectionController.class);

    private final PulsarDiagnosticAgent agent;

    public InspectionController(PulsarDiagnosticAgent agent) {
        this.agent = agent;
    }

    /**
     * Perform a full cluster inspection
     */
    @PostMapping
    public String inspect(@Valid @RequestBody(required = false) InspectionRequest request) {
        log.info("Received inspection request");

        if (request != null && request.focusAreas() != null) {
            return agent.inspect(request.focusAreas());
        }

        return agent.inspect();
    }

    /**
     * Perform a full cluster inspection (GET version)
     */
    @GetMapping
    public String fullInspection() {
        log.info("Performing full inspection");
        return agent.inspect();
    }

    /**
     * Quick health check
     */
    @GetMapping("/quick")
    public String quickHealthCheck() {
        log.info("Performing quick health check");
        return agent.quickHealthCheck();
    }

    /**
     * Inspect brokers
     */
    @GetMapping("/brokers")
    public String inspectBrokers() {
        log.info("Inspecting brokers");
        return agent.inspect("brokers");
    }

    /**
     * Inspect bookies
     */
    @GetMapping("/bookies")
    public String inspectBookies() {
        log.info("Inspecting bookies");
        return agent.inspect("bookies");
    }

    /**
     * Inspect topics
     */
    @GetMapping("/topics")
    public String inspectTopics() {
        log.info("Inspecting topics");
        return agent.inspect("topics");
    }

    /**
     * Inspect performance
     */
    @GetMapping("/performance")
    public String inspectPerformance() {
        log.info("Inspecting performance");
        return agent.inspect("performance");
    }

    /**
     * Generate maintenance checklist
     */
    @GetMapping("/maintenance-checklist")
    public String generateMaintenanceChecklist() {
        log.info("Generating maintenance checklist");
        return agent.generateMaintenanceChecklist();
    }
}