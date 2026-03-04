package com.pulsar.diagnostic.web.controller;

import com.pulsar.diagnostic.agent.agent.PulsarDiagnosticAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for skill-based operations.
 * Skills are now handled by SkillsTool integrated with ChatClient.
 */
@RestController
@RequestMapping("/api/skills")
@Tag(name = "Skills", description = "Endpoints for executing Pulsar diagnostic skills")
public class SkillController {

    private static final Logger log = LoggerFactory.getLogger(SkillController.class);

    private final PulsarDiagnosticAgent agent;
    private final ChatClient chatClient;

    public SkillController(PulsarDiagnosticAgent agent, ChatClient chatClient) {
        this.agent = agent;
        this.chatClient = chatClient;
    }

    /**
     * List all available skills (now handled by SkillsTool)
     */
    @GetMapping
    @Operation(summary = "List available skills", description = "Returns information about available skills")
    @ApiResponse(responseCode = "200", description = "Skills info retrieved")
    public Map<String, Object> listSkills() {
        log.info("Listing available skills info");
        return Map.of(
            "message", "Skills are managed by SkillsTool and integrated with ChatClient",
            "availableEndpoints", List.of(
                "/api/chat - Chat with the agent (tools are called automatically)",
                "/api/diagnose - Run diagnostics",
                "/api/inspection - Run inspections"
            )
        );
    }

    /**
     * Execute a skill with a query - delegates to chat endpoint
     */
    @PostMapping("/{skillName}/execute")
    @Operation(summary = "Execute a skill", description = "Execute a specific skill using natural language")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Skill executed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Skill not found")
    })
    public SkillExecutionResponse executeSkill(
            @Parameter(description = "Skill name")
            @PathVariable String skillName,
            @RequestBody(required = false) Map<String, Object> parameters) {
        log.info("Executing skill: {} with parameters: {}", skillName, parameters);

        // Build a prompt for the skill
        String prompt = buildSkillPrompt(skillName, parameters);

        // Use chat which has SkillsTool integrated
        String result = agent.chat(prompt);

        return new SkillExecutionResponse(skillName, true, result, null);
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
    public SkillExecutionResponse executeBestMatch(
            @RequestBody @NotBlank @Size(max = 5000) String query) {
        log.info("Finding best match for query: {}", query.substring(0, Math.min(100, query.length())));

        // Use chat which has SkillsTool integrated
        String result = agent.chat(query);

        return new SkillExecutionResponse("auto-selected", true, result, null);
    }

    /**
     * Build a skill prompt from skill name and parameters
     */
    private String buildSkillPrompt(String skillName, Map<String, Object> parameters) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Execute the ").append(skillName.replace("-", " ")).append(" task.");

        if (parameters != null && !parameters.isEmpty()) {
            prompt.append("\n\nParameters:\n");
            parameters.forEach((key, value) ->
                prompt.append("- ").append(key).append(": ").append(value).append("\n"));
        }

        return prompt.toString();
    }

    /**
     * Response DTO for skill execution
     */
    public record SkillExecutionResponse(
            String skillName,
            boolean success,
            String output,
            String error
    ) {}
}