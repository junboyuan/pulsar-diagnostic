package com.pulsar.diagnostic.agent.skill.declarative;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SkillDefinition
 */
class SkillDefinitionTest {

    @Test
    @DisplayName("Should return correct key from skill name")
    void getKey_shouldConvertNameToKey() {
        SkillDefinition skill = new SkillDefinition(
                "backlog-diagnosis",
                "Test description",
                "diagnosis",
                "Test prompt",
                List.of(),
                null,
                List.of()
        );

        assertEquals("backlog_diagnosis", skill.getKey());
    }

    @Test
    @DisplayName("Should return null key when name is null")
    void getKey_shouldReturnNullWhenNameIsNull() {
        SkillDefinition skill = new SkillDefinition(
                null,
                "Test description",
                "diagnosis",
                "Test prompt",
                List.of(),
                null,
                List.of()
        );

        assertNull(skill.getKey());
    }

    @Test
    @DisplayName("Should return 0.0 for null query")
    void canHandle_shouldReturnZeroForNullQuery() {
        SkillDefinition skill = new SkillDefinition(
                "test-skill",
                "Test description",
                "test",
                "Test prompt",
                List.of(),
                null,
                List.of()
        );

        assertEquals(0.0, skill.canHandle(null));
    }

    @Test
    @DisplayName("Should return 0.0 for empty query")
    void canHandle_shouldReturnZeroForEmptyQuery() {
        SkillDefinition skill = new SkillDefinition(
                "test-skill",
                "Test description",
                "test",
                "Test prompt",
                List.of(),
                null,
                List.of()
        );

        assertEquals(0.0, skill.canHandle(""));
    }

    @Test
    @DisplayName("Should match skill name in query")
    void canHandle_shouldMatchSkillName() {
        SkillDefinition skill = new SkillDefinition(
                "backlog-diagnosis",
                "Test description",
                "diagnosis",
                "Test prompt",
                List.of(),
                null,
                List.of()
        );

        double score = skill.canHandle("I need backlog diagnosis for my topic");

        assertTrue(score >= 0.3);
    }

    @Test
    @DisplayName("Should match description keywords")
    void canHandle_shouldMatchDescriptionKeywords() {
        SkillDefinition skill = new SkillDefinition(
                "test-skill",
                "diagnose message backlog issues in pulsar topics",
                "diagnosis",
                "Test prompt",
                List.of(),
                null,
                List.of()
        );

        double score = skill.canHandle("how to diagnose message issues");

        assertTrue(score > 0.0);
    }

    @Test
    @DisplayName("Should match example prompts")
    void canHandle_shouldMatchExamplePrompts() {
        SkillDefinition skill = new SkillDefinition(
                "test-skill",
                "Test description",
                "test",
                "Test prompt",
                List.of(),
                null,
                List.of("check backlog", "diagnose lag")
        );

        double score = skill.canHandle("I want to check backlog on my topic");

        assertTrue(score >= 0.2);
    }

    @Test
    @DisplayName("Should cap score at 1.0")
    void canHandle_shouldCapScoreAtOne() {
        SkillDefinition skill = new SkillDefinition(
                "backlog-diagnosis",
                "backlog diagnosis message pulsar topic consumer subscription",
                "diagnosis",
                "Test prompt",
                List.of(),
                null,
                List.of("backlog diagnosis", "check backlog", "message lag")
        );

        double score = skill.canHandle("backlog diagnosis check message pulsar topic");

        assertTrue(score <= 1.0);
    }

    @Test
    @DisplayName("Should be case insensitive")
    void canHandle_shouldBeCaseInsensitive() {
        SkillDefinition skill = new SkillDefinition(
                "BackLog-Diagnosis",
                "Test Description",
                "diagnosis",
                "Test prompt",
                List.of(),
                null,
                List.of("Check Backlog")
        );

        double score = skill.canHandle("BACKLOG DIAGNOSIS check BACKLOG");

        assertTrue(score > 0.0);
    }

    @Test
    @DisplayName("Should not match short words")
    void canHandle_shouldNotMatchShortWords() {
        SkillDefinition skill = new SkillDefinition(
                "test-skill",
                "a an the is of to in",
                "test",
                "Test prompt",
                List.of(),
                null,
                List.of()
        );

        double score = skill.canHandle("a an the is of to in");

        assertEquals(0.0, score);
    }

    @Test
    @DisplayName("Should create skill with all fields")
    void shouldCreateSkillWithAllFields() {
        SkillDefinition.SkillParameterDef param = new SkillDefinition.SkillParameterDef(
                "topic", "string", "Topic name", null
        );
        SkillDefinition.SkillParameters params = new SkillDefinition.SkillParameters(
                List.of(param), List.of()
        );

        SkillDefinition skill = new SkillDefinition(
                "test-skill",
                "Test description",
                "test",
                "Test prompt",
                List.of("tool1", "tool2"),
                params,
                List.of("example prompt")
        );

        assertEquals("test-skill", skill.name());
        assertEquals("Test description", skill.description());
        assertEquals("test", skill.category());
        assertEquals("Test prompt", skill.systemPrompt());
        assertEquals(2, skill.availableTools().size());
        assertEquals(1, skill.parameters().required().size());
        assertEquals(1, skill.examplePrompts().size());
    }
}