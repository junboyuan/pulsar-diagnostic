package com.pulsar.diagnostic.agent.skill.config;

import com.pulsar.diagnostic.agent.skill.Skill;
import com.pulsar.diagnostic.agent.skill.SkillRegistry;
import com.pulsar.diagnostic.agent.skill.impl.*;
import com.pulsar.diagnostic.agent.subagent.SubAgentRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for the skill framework.
 * Registers all available skills and workflows.
 */
@Configuration
public class SkillConfiguration implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SkillConfiguration.class);

    private final SkillRegistry skillRegistry;
    private final List<Skill> skills;

    public SkillConfiguration(SkillRegistry skillRegistry, List<Skill> skills) {
        this.skillRegistry = skillRegistry;
        this.skills = skills;
    }

    /**
     * Register all skills after application is ready
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Registering {} skills...", skills.size());
        skillRegistry.registerAll(skills);

        for (Skill skill : skills) {
            log.info("  - {} : {}", skill.getName(), skill.getDescription());
        }
    }

    /**
     * Skill beans - automatically collected and registered
     */
    @Bean
    public BacklogDiagnosisSkill backlogDiagnosisSkill() {
        return new BacklogDiagnosisSkill();
    }

    @Bean
    public PerformanceAnalysisSkill performanceAnalysisSkill() {
        return new PerformanceAnalysisSkill();
    }

    @Bean
    public ConnectivityTroubleshootSkill connectivityTroubleshootSkill() {
        return new ConnectivityTroubleshootSkill();
    }

    @Bean
    public CapacityPlanningSkill capacityPlanningSkill() {
        return new CapacityPlanningSkill();
    }

    @Bean
    public TopicConsultationSkill topicConsultationSkill() {
        return new TopicConsultationSkill();
    }

    @Bean
    public ClusterHealthCheckSkill clusterHealthCheckSkill(SubAgentRegistry subAgentRegistry) {
        return new ClusterHealthCheckSkill(subAgentRegistry);
    }
}