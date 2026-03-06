package com.pulsar.diagnostic.agent.subagent.config;

import com.pulsar.diagnostic.agent.subagent.SubAgent;
import com.pulsar.diagnostic.agent.subagent.SubAgentRegistry;
import com.pulsar.diagnostic.agent.subagent.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

/**
 * Configuration for the SubAgent framework.
 * Registers all available subagents.
 */
@Configuration
public class SubAgentConfiguration implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SubAgentConfiguration.class);

    private final SubAgentRegistry subAgentRegistry;
    private final List<SubAgent> subAgents;

    public SubAgentConfiguration(SubAgentRegistry subAgentRegistry, @Lazy List<SubAgent> subAgents) {
        this.subAgentRegistry = subAgentRegistry;
        this.subAgents = subAgents;
    }

    /**
     * Register all subagents after application is ready
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Registering {} subagents...", subAgents.size());
        subAgentRegistry.registerAll(subAgents);

        for (SubAgent agent : subAgents) {
            log.info("  - {} : {} [capabilities: {}]",
                    agent.getId(),
                    agent.getName(),
                    agent.getCapabilities());
        }
    }

    /**
     * SubAgent beans - automatically collected and registered
     */
    @Bean
    public LogAnalysisSubAgent logAnalysisSubAgent() {
        return new LogAnalysisSubAgent();
    }

    @Bean
    public MetricsQuerySubAgent metricsQuerySubAgent() {
        return new MetricsQuerySubAgent();
    }

    @Bean
    public ClusterInfoSubAgent clusterInfoSubAgent() {
        return new ClusterInfoSubAgent();
    }

    @Bean
    public TopicDiagnosisSubAgent topicDiagnosisSubAgent() {
        return new TopicDiagnosisSubAgent();
    }
}