package com.pulsar.mcp.mock.controller;

import com.pulsar.mcp.mock.model.McpToolInfo;
import com.pulsar.mcp.mock.model.McpToolRequest;
import com.pulsar.mcp.mock.model.McpToolResponse;
import com.pulsar.mcp.mock.model.McpToolsListResponse;
import com.pulsar.mcp.mock.service.MockDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * REST controller for MCP tool endpoints.
 */
@RestController
@RequestMapping("/api/tools")
public class McpToolController {

    private static final Logger log = LoggerFactory.getLogger(McpToolController.class);

    private final Map<String, MockDataService> toolServices;

    public McpToolController(List<MockDataService> services) {
        this.toolServices = services.stream()
                .collect(Collectors.toMap(MockDataService::getToolName, Function.identity()));
        log.info("Registered {} MCP mock tools: {}", toolServices.size(), toolServices.keySet());
    }

    /**
     * Get list of all available tools.
     */
    @GetMapping
    public ResponseEntity<McpToolsListResponse> getTools() {
        List<McpToolInfo> tools = toolServices.values().stream()
                .map(service -> new McpToolInfo(
                        service.getToolName(),
                        service.getToolDescription(),
                        service.getParametersSchema()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new McpToolsListResponse(tools));
    }

    /**
     * Call a specific tool.
     */
    @PostMapping("/{toolName}")
    public ResponseEntity<McpToolResponse> callTool(
            @PathVariable String toolName,
            @RequestBody(required = false) McpToolRequest request) {

        log.debug("Calling tool: {} with arguments: {}", toolName,
                request != null ? request.getArguments() : null);

        MockDataService service = toolServices.get(toolName);
        if (service == null) {
            log.warn("Unknown tool requested: {}", toolName);
            return ResponseEntity.badRequest()
                    .body(McpToolResponse.error("Unknown tool: " + toolName));
        }

        try {
            Map<String, Object> arguments = request != null ? request.getArguments() : null;
            String result = service.execute(arguments);
            return ResponseEntity.ok(McpToolResponse.success(result));
        } catch (Exception e) {
            log.error("Error executing tool {}: {}", toolName, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(McpToolResponse.error("Error executing tool: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "toolsCount", toolServices.size(),
                "tools", toolServices.keySet()
        ));
    }
}