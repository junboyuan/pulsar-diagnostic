package com.pulsar.diagnostic.agent.skill.impl;

import com.pulsar.diagnostic.agent.skill.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Skill for troubleshooting connectivity issues.
 * Diagnoses network, broker, and client connection problems.
 */
@Component
public class ConnectivityTroubleshootSkill extends AbstractSkill {

    public static final String NAME = "connectivity-troubleshoot";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Troubleshoot Pulsar connectivity issues including broker availability, network problems, and client connections";
    }

    @Override
    public String getCategory() {
        return "troubleshooting";
    }

    @Override
    public List<SkillParameter> getOptionalParameters() {
        return List.of(
                SkillParameter.optional("broker", "Specific broker to check (optional)", "string", null),
                SkillParameter.optional("client", "Client address to investigate (optional)", "string", null),
                SkillParameter.optional("timeout", "Connection timeout in seconds", "integer", "30")
        );
    }

    @Override
    public List<String> getExamplePrompts() {
        return List.of(
                "connection issue",
                "cannot connect to pulsar",
                "broker unavailable",
                "client connection failed",
                "network problem",
                "timeout error"
        );
    }

    @Override
    protected SkillResult doExecute(SkillContext context) {
        String broker = context.getParameter("broker");
        String client = context.getParameter("client");
        int timeout = context.getParameter("timeout", 30);

        context.log("Starting connectivity troubleshooting...");
        context.log("Broker: " + broker + ", Client: " + client + ", Timeout: " + timeout + "s");

        List<SkillResult.Finding> findings = new ArrayList<>();
        List<SkillResult.Recommendation> recommendations = new ArrayList<>();
        StringBuilder output = new StringBuilder();

        // Step 1: Check cluster health
        context.log("Step 1: Checking cluster health...");
        String healthCheck = context.tools().performHealthCheck();
        output.append("=== Cluster Health ===\n").append(healthCheck).append("\n");
        analyzeHealthCheck(healthCheck, findings);

        // Step 2: Check active brokers
        context.log("Step 2: Checking active brokers...");
        String brokers = context.tools().getActiveBrokers();
        output.append("\n=== Active Brokers ===\n").append(brokers).append("\n");
        analyzeBrokerStatus(brokers, findings, recommendations);

        // Step 3: Check bookies
        context.log("Step 3: Checking bookies...");
        String bookies = context.tools().getBookies();
        output.append("\n=== Bookies ===\n").append(bookies).append("\n");
        analyzeBookieStatus(bookies, findings, recommendations);

        // Step 4: Check connection metrics
        context.log("Step 4: Analyzing connection metrics...");
        String metrics = context.tools().getClusterMetrics();
        output.append("\n=== Connection Metrics ===\n").append(metrics).append("\n");
        analyzeConnectionMetrics(metrics, findings, recommendations);

        // Step 5: Check logs for connection errors
        context.log("Step 5: Checking logs for connection errors...");
        String brokerLogs = context.tools().analyzeBrokerLogs(300);
        output.append("\n=== Recent Log Errors ===\n").append(brokerLogs).append("\n");
        checkForConnectionErrors(brokerLogs, findings);

        // Step 6: Run connection diagnostics via MCP
        context.log("Step 6: Running comprehensive connection diagnosis...");
        String connectionDiag = context.mcp().diagnoseProblem("connection", null);
        output.append("\n=== Connection Diagnosis ===\n").append(connectionDiag).append("\n");

        // Step 7: Get troubleshooting knowledge
        if (context.knowledge().isReady()) {
            context.log("Step 7: Getting troubleshooting guidance...");
            String guide = context.knowledge().getTroubleshootingGuide("connection");
            if (guide != null && !guide.isEmpty()) {
                output.append("\n=== Troubleshooting Guide ===\n").append(guide).append("\n");
            }
        }

        // Add recommendations based on findings
        addConnectivityRecommendations(findings, recommendations);

        return SkillResult.builder()
                .success(true)
                .output(output.toString())
                .findings(findings)
                .recommendations(recommendations)
                .metadata(Map.of(
                        "broker", broker != null ? broker : "all",
                        "client", client != null ? client : "all",
                        "findingsCount", findings.size()
                ))
                .build();
    }

    private void analyzeHealthCheck(String healthCheck, List<SkillResult.Finding> findings) {
        if (healthCheck == null) return;
        String lower = healthCheck.toLowerCase();

        if (lower.contains("critical") || lower.contains("unhealthy")) {
            findings.add(SkillResult.Finding.critical(
                    "Cluster Health Critical",
                    "Cluster is in an unhealthy state",
                    "cluster"
            ));
        } else if (lower.contains("warning")) {
            findings.add(SkillResult.Finding.warning(
                    "Cluster Health Warning",
                    "Cluster has some health warnings",
                    "cluster"
            ));
        }
    }

    private void analyzeBrokerStatus(String brokers, List<SkillResult.Finding> findings,
                                      List<SkillResult.Recommendation> recommendations) {
        if (brokers == null) return;
        String lower = brokers.toLowerCase();

        if (lower.contains("no active brokers") || lower.contains("0 active")) {
            findings.add(SkillResult.Finding.critical(
                    "No Active Brokers",
                    "No brokers are currently active in the cluster",
                    "cluster"
            ));
            recommendations.add(SkillResult.Recommendation.urgent(
                    "Start broker processes",
                    "No brokers are running - check broker logs and configuration"
            ));
        } else if (lower.contains("unhealthy") || lower.contains("down")) {
            findings.add(SkillResult.Finding.error(
                    "Unhealthy Broker Detected",
                    "One or more brokers are not healthy",
                    "broker"
            ));
        }
    }

    private void analyzeBookieStatus(String bookies, List<SkillResult.Finding> findings,
                                      List<SkillResult.Recommendation> recommendations) {
        if (bookies == null) return;
        String lower = bookies.toLowerCase();

        if (lower.contains("no bookies") || lower.contains("0 bookie")) {
            findings.add(SkillResult.Finding.critical(
                    "No Bookies Available",
                    "No bookies are available - data persistence is at risk",
                    "bookkeeper"
            ));
            recommendations.add(SkillResult.Recommendation.urgent(
                    "Start bookie processes",
                    "BookKeeper nodes are not running"
            ));
        } else if (lower.contains("read-only")) {
            findings.add(SkillResult.Finding.warning(
                    "Read-Only Bookies",
                    "Some bookies are in read-only mode",
                    "bookkeeper"
            ));
            recommendations.add(SkillResult.Recommendation.high(
                    "Check bookie disk space",
                    "Read-only mode often indicates disk space issues"
            ));
        }
    }

    private void analyzeConnectionMetrics(String metrics, List<SkillResult.Finding> findings,
                                           List<SkillResult.Recommendation> recommendations) {
        String connStr = extractValue(metrics, "Total Connections");
        if (connStr != null) {
            double connections = parseDouble(connStr, -1);
            if (connections == 0) {
                findings.add(SkillResult.Finding.warning(
                        "No Active Connections",
                        "Cluster has zero active client connections",
                        "cluster"
                ));
                recommendations.add(SkillResult.Recommendation.medium(
                        "Verify client configuration",
                        "Check client broker service URL and network connectivity"
                ));
            }
        }

        String producersStr = extractValue(metrics, "Total Producers");
        String consumersStr = extractValue(metrics, "Total Consumers");

        if (producersStr != null && consumersStr != null) {
            double producers = parseDouble(producersStr, 0);
            double consumers = parseDouble(consumersStr, 0);

            if (producers > 0 && consumers == 0) {
                findings.add(SkillResult.Finding.info(
                        "No Consumers",
                        "There are producers but no consumers connected",
                        "cluster"
                ));
            }
        }
    }

    private void checkForConnectionErrors(String logs, List<SkillResult.Finding> findings) {
        if (logs == null) return;
        String lower = logs.toLowerCase();

        if (lower.contains("connection refused")) {
            findings.add(SkillResult.Finding.error(
                    "Connection Refused",
                    "Connection refused errors found in logs",
                    "network"
            ));
        }

        if (lower.contains("connection reset") || lower.contains("connection closed")) {
            findings.add(SkillResult.Finding.warning(
                    "Connection Resets",
                    "Connection reset or closed errors detected",
                    "network"
            ));
        }

        if (lower.contains("timeout")) {
            findings.add(SkillResult.Finding.warning(
                    "Timeout Errors",
                    "Timeout errors found in logs",
                    "network"
            ));
        }

        if (lower.contains("authentication failed") || lower.contains("authorization failed")) {
            findings.add(SkillResult.Finding.error(
                    "Authentication/Authorization Error",
                    "Authentication or authorization failures detected",
                    "security"
            ));
        }
    }

    private void addConnectivityRecommendations(List<SkillResult.Finding> findings,
                                                  List<SkillResult.Recommendation> recommendations) {
        boolean hasNetworkIssue = findings.stream().anyMatch(f -> f.affectedResource().equals("network"));
        boolean hasBrokerIssue = findings.stream().anyMatch(f -> f.affectedResource().equals("broker"));
        boolean hasSecurityIssue = findings.stream().anyMatch(f -> f.affectedResource().equals("security"));

        if (hasNetworkIssue) {
            recommendations.add(SkillResult.Recommendation.high(
                    "Check network configuration",
                    "Verify firewall rules, network routes, and DNS resolution"
            ));
            recommendations.add(SkillResult.Recommendation.medium(
                    "Check broker advertised address",
                    "Ensure brokers are advertising correct addresses"
            ));
        }

        if (hasBrokerIssue) {
            recommendations.add(SkillResult.Recommendation.high(
                    "Restart affected brokers",
                    "Try restarting brokers that are not healthy"
            ));
        }

        if (hasSecurityIssue) {
            recommendations.add(SkillResult.Recommendation.high(
                    "Verify authentication configuration",
                    "Check authentication/authorization settings on broker and client"
            ));
        }

        if (findings.isEmpty()) {
            recommendations.add(SkillResult.Recommendation.low(
                    "Connectivity looks healthy",
                    "No immediate connectivity issues detected"
            ));
        }
    }

    @Override
    public double canHandle(String query) {
        String lower = query.toLowerCase();
        double score = 0.0;

        if (lower.contains("connect")) score += 0.4;
        if (lower.contains("network")) score += 0.3;
        if (lower.contains("broker") && (lower.contains("down") || lower.contains("unavailable"))) score += 0.4;
        if (lower.contains("timeout")) score += 0.3;
        if (lower.contains("refused")) score += 0.3;
        if (lower.contains("unreachable")) score += 0.3;
        if (lower.contains("client") && lower.contains("issue")) score += 0.2;

        return Math.min(score, 1.0);
    }
}