package com.pulsar.diagnostic.agent.skill.declarative;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SkillExecutionResult
 */
class SkillExecutionResultTest {

    @Test
    @DisplayName("Should create successful result")
    void success_shouldCreateSuccessfulResult() {
        SkillExecutionResult result = SkillExecutionResult.success("test-skill", "Test output", 100L);

        assertTrue(result.success());
        assertEquals("test-skill", result.skillName());
        assertEquals("Test output", result.output());
        assertNull(result.error());
        assertEquals(100L, result.executionTimeMs());
        assertTrue(result.metadata().isEmpty());
    }

    @Test
    @DisplayName("Should create successful result with metadata")
    void success_shouldCreateSuccessfulResultWithMetadata() {
        Map<String, Object> metadata = Map.of("key1", "value1", "key2", 123);

        SkillExecutionResult result = SkillExecutionResult.success("test-skill", "Test output", 100L, metadata);

        assertTrue(result.success());
        assertEquals(2, result.metadata().size());
        assertEquals("value1", result.metadata().get("key1"));
        assertEquals(123, result.metadata().get("key2"));
    }

    @Test
    @DisplayName("Should create failure result")
    void failure_shouldCreateFailureResult() {
        SkillExecutionResult result = SkillExecutionResult.failure("Something went wrong");

        assertFalse(result.success());
        assertNull(result.skillName());
        assertNull(result.output());
        assertEquals("Something went wrong", result.error());
        assertEquals(0L, result.executionTimeMs());
    }

    @Test
    @DisplayName("Should create failure result with skill name")
    void failure_shouldCreateFailureResultWithSkillName() {
        SkillExecutionResult result = SkillExecutionResult.failure("test-skill", "Execution failed");

        assertFalse(result.success());
        assertEquals("test-skill", result.skillName());
        assertEquals("Execution failed", result.error());
    }

    @Test
    @DisplayName("Should detect output presence")
    void hasOutput_shouldDetectOutputPresence() {
        SkillExecutionResult withOutput = SkillExecutionResult.success("test", "output", 0L);
        SkillExecutionResult withoutOutput = SkillExecutionResult.success("test", "", 0L);
        SkillExecutionResult nullOutput = SkillExecutionResult.success("test", null, 0L);

        assertTrue(withOutput.hasOutput());
        assertFalse(withoutOutput.hasOutput());
        assertFalse(nullOutput.hasOutput());
    }

    @Test
    @DisplayName("Should return output or error message")
    void getOutputOrError_shouldReturnCorrectMessage() {
        SkillExecutionResult success = SkillExecutionResult.success("test", "Success output", 0L);
        SkillExecutionResult failure = SkillExecutionResult.failure("Error message");

        assertEquals("Success output", success.getOutputOrError());
        assertEquals("Error: Error message", failure.getOutputOrError());
    }

    @Test
    @DisplayName("Should handle empty metadata")
    void shouldHandleEmptyMetadata() {
        SkillExecutionResult result = SkillExecutionResult.success("test", "output", 0L);

        assertNotNull(result.metadata());
        assertTrue(result.metadata().isEmpty());
    }
}