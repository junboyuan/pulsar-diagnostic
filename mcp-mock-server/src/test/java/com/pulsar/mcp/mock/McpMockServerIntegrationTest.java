package com.pulsar.mcp.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for MCP Mock Server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class McpMockServerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testHealthEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/tools/health", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals(18, response.getBody().get("toolsCount"));
    }

    @Test
    void testListTools() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/tools", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        List<Map<String, Object>> tools = (List<Map<String, Object>>) response.getBody().get("tools");
        assertNotNull(tools);
        assertEquals(18, tools.size());

        // Verify some expected tools
        List<String> toolNames = tools.stream()
                .map(t -> (String) t.get("name"))
                .toList();
        assertTrue(toolNames.contains("inspect_cluster"));
        assertTrue(toolNames.contains("get_broker_metrics"));
        assertTrue(toolNames.contains("get_topic_backlog"));
    }

    @Test
    void testInspectCluster() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = Map.of(
            "arguments", Map.of("components", List.of("brokers"))
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/tools/inspect_cluster",
            entity,
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("result"));
        assertNull(response.getBody().get("error"));
    }

    @Test
    void testGetBrokerMetrics() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = Map.of("arguments", Map.of());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/tools/get_broker_metrics",
            entity,
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        String result = (String) response.getBody().get("result");
        assertNotNull(result);
        assertTrue(result.contains("brokerId"));
        assertTrue(result.contains("cpuUsage"));
        assertTrue(result.contains("messagesInPerSecond"));
    }

    @Test
    void testGetTopicBacklog() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = Map.of(
            "arguments", Map.of("topic", "persistent://public/default/test-topic")
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/tools/get_topic_backlog",
            entity,
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        String result = (String) response.getBody().get("result");
        assertNotNull(result);
        assertTrue(result.contains("backlog"));
        assertTrue(result.contains("subscriptions"));
    }

    @Test
    void testCheckDiskSpace() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = Map.of("arguments", Map.of());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/tools/check_disk_space",
            entity,
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        String result = (String) response.getBody().get("result");
        assertNotNull(result);
        assertTrue(result.contains("totalSpace"));
        assertTrue(result.contains("usagePercentage"));
    }

    @Test
    void testUnknownTool() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = Map.of("arguments", Map.of());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/tools/unknown_tool",
            entity,
            Map.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}