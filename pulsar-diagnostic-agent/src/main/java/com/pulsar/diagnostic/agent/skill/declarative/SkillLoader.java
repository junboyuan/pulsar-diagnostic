package com.pulsar.diagnostic.agent.skill.declarative;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads skill definitions from SKILL.md files.
 *
 * Skill files use the Claude Code SKILL.md format with YAML frontmatter:
 *
 * ---
 * name: skill-name
 * description: Skill description
 * ---
 *
 * # Skill Title
 *
 * Skill content in markdown...
 */
@Component
public class SkillLoader {

    private static final Logger log = LoggerFactory.getLogger(SkillLoader.class);

    private static final String SKILLS_LOCATION = "classpath:skills/*/SKILL.md";
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\s*\n(.*?)\n---\\s*\n", Pattern.DOTALL);
    private static final Pattern NAME_PATTERN = Pattern.compile("name:\\s*(.+)");
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("description:\\s*\"?([^\"\\n]+)\"?");
    private static final Pattern ALLOWED_TOOLS_PATTERN = Pattern.compile("allowed-tools:\\s*(.+)");

    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();

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
            String content = readResourceContent(resource);
            SkillDefinition skill = parseSkillDefinition(content, resource);

            if (skill.name() == null || skill.name().isEmpty()) {
                log.warn("Skipping skill without name: {}", resource.getFilename());
                return;
            }

            skills.put(skill.getKey(), skill);
            log.info("Loaded skill: {} - {}", skill.name(), skill.description());

        } catch (IOException e) {
            log.error("Failed to load skill from {}: {}", resource.getFilename(), e.getMessage());
        }
    }

    private String readResourceContent(Resource resource) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private SkillDefinition parseSkillDefinition(String content, Resource resource) {
        // Parse frontmatter
        String name = null;
        String description = null;
        List<String> allowedTools = new ArrayList<>();

        Matcher frontmatterMatcher = FRONTMATTER_PATTERN.matcher(content);
        if (frontmatterMatcher.find()) {
            String frontmatter = frontmatterMatcher.group(1);

            Matcher nameMatcher = NAME_PATTERN.matcher(frontmatter);
            if (nameMatcher.find()) {
                name = nameMatcher.group(1).trim();
            }

            Matcher descMatcher = DESCRIPTION_PATTERN.matcher(frontmatter);
            if (descMatcher.find()) {
                description = descMatcher.group(1).trim();
            }

            // Parse allowed-tools from frontmatter
            Matcher toolsMatcher = ALLOWED_TOOLS_PATTERN.matcher(frontmatter);
            if (toolsMatcher.find()) {
                String toolsStr = toolsMatcher.group(1).trim();
                // Parse comma-separated tool names
                for (String tool : toolsStr.split(",")) {
                    String trimmed = tool.trim();
                    if (!trimmed.isEmpty()) {
                        allowedTools.add(trimmed);
                    }
                }
            }
        }

        // Extract content after frontmatter
        String body = content;
        frontmatterMatcher.reset(); // Reset matcher to find frontmatter again
        if (frontmatterMatcher.find()) {
            body = content.substring(frontmatterMatcher.end());
        }

        // Extract category from content (look for ## Overview or first heading)
        String category = extractCategory(body);

        // If no allowed-tools in frontmatter, extract from content as fallback
        List<String> availableTools = allowedTools.isEmpty() ? extractTools(body) : allowedTools;

        // Extract example prompts from content
        List<String> examplePrompts = extractExamplePrompts(body, name);

        return new SkillDefinition(
                name,
                description,
                category,
                body.trim(),
                availableTools,
                null, // parameters - can be extended
                examplePrompts
        );
    }

    private String extractCategory(String content) {
        // Try to find category from content patterns
        if (content.contains("diagnos") || content.contains("troubleshoot")) {
            return "diagnosis";
        }
        if (content.contains("performance") || content.contains("throughput")) {
            return "analysis";
        }
        if (content.contains("planning") || content.contains("capacity")) {
            return "planning";
        }
        if (content.contains("consultation") || content.contains("design")) {
            return "consultation";
        }
        return "general";
    }

    private List<String> extractTools(String content) {
        List<String> tools = new ArrayList<>();

        // Look for tool names in code blocks or tables
        Pattern toolPattern = Pattern.compile("`([a-zA-Z][a-zA-Z0-9]*)\\(\\)`");
        Matcher matcher = toolPattern.matcher(content);

        Set<String> seen = new HashSet<>();
        while (matcher.find()) {
            String tool = matcher.group(1);
            if (!seen.contains(tool)) {
                tools.add(tool);
                seen.add(tool);
            }
        }

        // Also look for tools in tables
        Pattern tablePattern = Pattern.compile("\\|\\s*`?([a-zA-Z][a-zA-Z0-9]*)`?\\s*\\|");
        Matcher tableMatcher = tablePattern.matcher(content);
        while (tableMatcher.find()) {
            String tool = tableMatcher.group(1);
            if (!seen.contains(tool) && !tool.equals("Tool") && !tool.equals("Purpose")) {
                tools.add(tool);
                seen.add(tool);
            }
        }

        return tools;
    }

    private List<String> extractExamplePrompts(String content, String skillName) {
        List<String> prompts = new ArrayList<>();

        // Add skill name as a potential match
        if (skillName != null) {
            prompts.add(skillName.replace("-", " "));
        }

        // Look for "When to Use" section
        Pattern whenPattern = Pattern.compile("## When to Use\\s*\n([\\s\\S]*?)(?=##|$)");
        Matcher matcher = whenPattern.matcher(content);
        if (matcher.find()) {
            String whenSection = matcher.group(1);
            // Extract bullet points
            Pattern bulletPattern = Pattern.compile("-\\s*(.+)");
            Matcher bulletMatcher = bulletPattern.matcher(whenSection);
            while (bulletMatcher.find()) {
                String prompt = bulletMatcher.group(1).trim();
                if (prompt.length() > 10 && prompt.length() < 100) {
                    prompts.add(prompt.toLowerCase());
                }
            }
        }

        return prompts;
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