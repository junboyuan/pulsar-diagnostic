package com.pulsar.diagnostic.web.controller;

import com.pulsar.diagnostic.agent.agent.PulsarDiagnosticAgent;
import com.pulsar.diagnostic.agent.skill.declarative.SkillDefinition;
import com.pulsar.diagnostic.agent.skill.declarative.SkillLoader;
import com.pulsar.diagnostic.agent.skill.declarative.SkillExecutor;
import com.pulsar.diagnostic.agent.skill.declarative.SkillExecutionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for skill-based operations.
 * Provides endpoints for the declarative skill framework.
 */
@RestController
@RequestMapping("/api/skills")
@Tag(name = "Skills", description = "Endpoints for executing Pulsar diagnostic skills")
public class SkillController {

    private static final Logger log = LoggerFactory.getLogger(SkillController.class);

    private final PulsarDiagnosticAgent agent;
    private final SkillLoader skillLoader;
    private final SkillExecutor skillExecutor;

    public SkillController(PulsarDiagnosticAgent agent,
                           SkillLoader skillLoader,
                           SkillExecutor skillExecutor) {
        this.agent = agent;
        this.skillLoader = skillLoader;
        this.skillExecutor = skillExecutor;
    }

    /**
     * List all available skills
     */
    @GetMapping
    @Operation(summary = "List available skills", description = "Returns all available skill definitions")
    @ApiResponse(responseCode = "200", description = "List of skills retrieved")
    public List<SkillDefinition> listSkills() {
        log.info("Listing all available skills");
        return agent.getAvailableSkills();
    }

    /**
     * Get a specific skill definition
     */
    @GetMapping("/{skillName}")
    @Operation(summary = "Get skill definition", description = "Returns the definition of a specific skill")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Skill definition found"),
            @ApiResponse(responseCode = "404", description = "Skill not found")
    })
    public SkillDefinition getSkill(
            @Parameter(description = "Skill name")
            @PathVariable String skillName) {
        log.info("Getting skill: {}", skillName);
        return skillLoader.getSkill(skillName)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + skillName));
    }

    /**
     * Execute a skill
     */
    @PostMapping("/{skillName}/execute")
    @Operation(summary = "Execute a skill", description = "Execute a specific skill with parameters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Skill executed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Skill not found")
    })
    public SkillExecutionResult executeSkill(
            @Parameter(description = "Skill name")
            @PathVariable String skillName,
            @RequestBody(required = false) Map<String, Object> parameters) {
        log.info("Executing skill: {} with parameters: {}", skillName, parameters);
        return skillExecutor.execute(skillName, parameters);
    }

    /**
     * Execute a skill with a query
     */
    @PostMapping("/{skillName}/query")
    @Operation(summary = "Execute skill with query", description = "Execute a skill with a natural language query")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Skill executed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Skill not found")
    })
    public SkillExecutionResult executeSkillWithQuery(
            @Parameter(description = "Skill name")
            @PathVariable String skillName,
            @RequestBody SkillQueryRequest request) {
        log.info("Executing skill: {} with query", skillName);
        return skillExecutor.execute(skillName, request.query(), request.parameters());
    }

    /**
     * Auto-select and execute the best matching skill
     */
    @PostMapping("/execute-best-match")
    @Operation(summary = "Auto-execute best matching skill", description = "Find and execute the best matching skill for the query")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Skill executed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query")
    })
    public SkillExecutionResult executeBestMatch(
            @RequestBody @NotBlank @Size(max = 5000) String query) {
        log.info("Finding best match for query: {}", query.substring(0, Math.min(100, query.length())));
        return skillExecutor.executeBestMatch(query, Map.of());
    }

    /**
     * Find skills matching a query
     */
    @PostMapping("/match")
    @Operation(summary = "Find matching skills", description = "Find all skills that match a query, sorted by confidence")
    @ApiResponse(responseCode = "200", description = "Matching skills found")
    public List<SkillLoader.SkillMatch> findMatchingSkills(
            @RequestBody @NotBlank @Size(max = 5000) String query) {
        log.info("Finding matching skills for query");
        return agent.findMatchingSkills(query);
    }

    /**
     * Request DTO for skill query
     */
    public record SkillQueryRequest(
            @NotBlank(message = "Query is required")
            @Size(max = 5000, message = "Query must not exceed 5000 characters")
            String query,
            Map<String, Object> parameters
    ) {
        public SkillQueryRequest {
            if (parameters == null) {
                parameters = Map.of();
            }
        }
    }
}