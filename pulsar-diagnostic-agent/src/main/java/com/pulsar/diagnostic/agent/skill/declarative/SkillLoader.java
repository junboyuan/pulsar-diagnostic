package com.pulsar.diagnostic.agent.skill.declarative;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads skill definitions from YAML files and manages the skill registry.
 */
@Component
public class SkillLoader {

    private static final Logger log = LoggerFactory.getLogger(SkillLoader.class);

    private static final String SKILLS_LOCATION = "classpath:skills/*.yaml";

    private final ObjectMapper yamlMapper;
    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();

    public SkillLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    @PostConstruct
    public void loadSkills() {
        log.info("Loading skill definitions from: {}", SKILLS_LOCATION);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(SKILLS_LOCATION);
            log.info("Found {} skill definition files", resources.length);

            for (Resource resource : resources) {
                loadSkill(resource);
            }

            log.info("Loaded {} skills: {}", skills.size(), skills.keySet());

        } catch (IOException e) {
            log.warn("Could not load skill definitions: {}", e.getMessage());
        }
    }

    private void loadSkill(Resource resource) {
        try {
            SkillDefinition skill = yamlMapper.readValue(resource.getInputStream(), SkillDefinition.class);

            if (skill.name() == null || skill.name().isEmpty()) {
                log.warn("Skipping skill without name: {}", resource.getFilename());
                return;
            }

            skills.put(skill.getKey(), skill);
            log.debug("Loaded skill: {} - {}", skill.name(), skill.description());

        } catch (IOException e) {
            log.error("Failed to load skill from {}: {}", resource.getFilename(), e.getMessage());
        }
    }

    /**
     * Get a skill by name
     */
    public Optional<SkillDefinition> getSkill(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String key = name.replace("-", "_");
        return Optional.ofNullable(skills.get(key));
    }

    /**
     * Get all loaded skills
     */
    public Collection<SkillDefinition> getAllSkills() {
        return Collections.unmodifiableCollection(skills.values());
    }

    /**
     * Find the best matching skill for a query
     */
    public Optional<SkillDefinition> findBestMatch(String query) {
        if (query == null || query.isEmpty()) {
            return Optional.empty();
        }

        SkillDefinition bestMatch = null;
        double bestScore = 0.0;

        for (SkillDefinition skill : skills.values()) {
            double score = skill.canHandle(query);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = skill;
            }
        }

        // Return match only if confidence is above threshold
        return bestScore > 0.3 ? Optional.ofNullable(bestMatch) : Optional.empty();
    }

    /**
     * Find all skills that can handle a query, sorted by confidence
     */
    public List<SkillMatch> findMatchingSkills(String query) {
        if (query == null || query.isEmpty()) {
            return List.of();
        }

        List<SkillMatch> matches = new ArrayList<>();

        for (SkillDefinition skill : skills.values()) {
            double score = skill.canHandle(query);
            if (score > 0.0) {
                matches.add(new SkillMatch(skill, score));
            }
        }

        matches.sort((a, b) -> Double.compare(b.score(), a.score()));
        return matches;
    }

    /**
     * Register a skill programmatically
     */
    public void registerSkill(SkillDefinition skill) {
        if (skill != null && skill.name() != null) {
            skills.put(skill.getKey(), skill);
            log.info("Registered skill: {}", skill.name());
        }
    }

    /**
     * Check if a skill exists
     */
    public boolean hasSkill(String name) {
        if (name == null) {
            return false;
        }
        return skills.containsKey(name.replace("-", "_"));
    }

    /**
     * Get skill count
     */
    public int getSkillCount() {
        return skills.size();
    }

    /**
     * Record for skill match results
     */
    public record SkillMatch(SkillDefinition skill, double score) {}
}