package com.pulsar.diagnostic.agent.skill;

import java.util.List;
import java.util.Map;

/**
 * Client interface for direct tool access within skills.
 */
public interface ToolSkillClient {

    // Cluster tools
    String getClusterInfo();
    String performHealthCheck();
    String getActiveBrokers();
    String getBookies();

    // Topic tools
    String getTopicInfo(String topicName);
    String getTopicStats(String topicName);
    String listTopicsInNamespace(String namespace);
    String getTopicSubscriptions(String topicName);
    String checkTopicBacklog(String topicName);

    // Metrics tools
    String getClusterMetrics();
    String queryMetric(String query);
    String getBrokerMetrics();
    String getAllMetrics();
    String checkMetricsAvailable();

    // Log tools
    String analyzeBrokerLogs(Integer maxLines);
    String analyzeBookieLogs(Integer maxLines);
    String searchAllLogs(String pattern, Integer maxResults);
    String getRecentErrors(Integer maxErrors);
    String tailLogFile(String filePath, Integer lines);

    // Diagnostic tools
    String diagnoseBacklogIssue(String resource, String resourceType);
    String diagnoseConnectionIssues();
    String diagnosePerformanceIssues();
    String runComprehensiveDiagnostic();

    // Inspection tools
    String performFullInspection();
    String performInspection(String focusArea);
    String quickHealthSnapshot();
}