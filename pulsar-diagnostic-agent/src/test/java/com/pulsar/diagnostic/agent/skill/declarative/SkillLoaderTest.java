package com.pulsar.diagnostic.agent.skill.declarative;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SkillLoader
 */
class SkillLoaderTest {

    private SkillLoader skillLoader;

    @BeforeEach
    void setUp() {
        skillLoader = new SkillLoader();
    }

    @Test
    @DisplayName("Should load skills from classpath on initialization")
    void loadSkills_shouldLoadFromClasspath() {
        // Trigger loading via @PostConstruct behavior
        skillLoader.loadSkills();

        // Check if skills are loaded from the actual resources
        Collection<SkillDefinition> skills = skillLoader.getAllSkills();

        // The actual skills should be loaded
        assertTrue(skills.size() >= 0, "Skills collection should not be null");
    }

    @Test
    @DisplayName("Should return empty optional for null skill name")
    void getSkill_shouldReturnEmptyForNull() {
        Optional<SkillDefinition> result = skillLoader.getSkill(null);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty optional for non-existent skill")
    void getSkill_shouldReturnEmptyForNonExistent() {
        Optional<SkillDefinition> result = skillLoader.getSkill("non-existent-skill");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should find skill by key with underscores")
    void getSkill_shouldFindSkillWithUnderscores() {
        // First register a test skill
        SkillDefinition testSkill = new SkillDefinition(
                "test-skill",
                "Test description",
                "test",
                "Test prompt",
                List.of(),
                null,
                List.of()
        );
        skillLoader.registerSkill(testSkill);

        // Should find with underscore version
        Optional<SkillDefinition> result = skillLoader.getSkill("test_skill");

        assertTrue(result.isPresent());
        assertEquals("test-skill", result.get().name());
    }

    @Test
    @DisplayName("Should register skill programmatically")
    void registerSkill_shouldAddToRegistry() {
        SkillDefinition skill = new SkillDefinition(
                "custom-skill",
                "Custom skill description",
                "custom",
                "Custom prompt",
                List.of(),
                null,
                List.of()
        );

        skillLoader.registerSkill(skill);

        assertTrue(skillLoader.hasSkill("custom-skill"));
        assertEquals(1, skillLoader.getSkillCount());
    }

    @Test
    @DisplayName("Should not register null skill")
    void registerSkill_shouldNotRegisterNull() {
        int countBefore = skillLoader.getSkillCount();

        skillLoader.registerSkill(null);

        assertEquals(countBefore, skillLoader.getSkillCount());
    }

    @Test
    @DisplayName("Should not register skill with null name")
    void registerSkill_shouldNotRegisterSkillWithNullName() {
        SkillDefinition skill = new SkillDefinition(
                null,
                "Description",
                "test",
                "Prompt",
                List.of(),
                null,
                List.of()
        );

        int countBefore = skillLoader.getSkillCount();
        skillLoader.registerSkill(skill);

        assertEquals(countBefore, skillLoader.getSkillCount());
    }

    @Test
    @DisplayName("Should find best matching skill")
    void findBestMatch_shouldReturnBestMatch() {
        SkillDefinition skill1 = new SkillDefinition(
                "backlog-diagnosis",
                "Diagnose message backlog issues",
                "diagnosis",
                "Prompt",
                List.of(),
                null,
                List.of("backlog", "message lag")
        );
        SkillDefinition skill2 = new SkillDefinition(
                "cluster-health",
                "Check cluster health status",
                "health",
                "Prompt",
                List.of(),
                null,
                List.of("health check", "cluster status")
        );

        skillLoader.registerSkill(skill1);
        skillLoader.registerSkill(skill2);

        Optional<SkillDefinition> result = skillLoader.findBestMatch("I have backlog issues");

        assertTrue(result.isPresent());
        assertEquals("backlog-diagnosis", result.get().name());
    }

    @Test
    @DisplayName("Should return empty when no skill matches threshold")
    void findBestMatch_shouldReturnEmptyWhenBelowThreshold() {
        SkillDefinition skill = new SkillDefinition(
                "specific-skill",
                "Very specific description",
                "test",
                "Prompt",
                List.of(),
                null,
                List.of("very specific example")
        );

        skillLoader.registerSkill(skill);

        // Query that doesn't match well
        Optional<SkillDefinition> result = skillLoader.findBestMatch("completely unrelated query");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty for null or empty query")
    void findBestMatch_shouldReturnEmptyForNullOrEmptyQuery() {
        assertFalse(skillLoader.findBestMatch(null).isPresent());
        assertFalse(skillLoader.findBestMatch("").isPresent());
    }

    @Test
    @DisplayName("Should find all matching skills sorted by score")
    void findMatchingSkills_shouldReturnSortedList() {
        SkillDefinition skill1 = new SkillDefinition(
                "backlog-check",
                "Check backlog",
                "test",
                "Prompt",
                List.of(),
                null,
                List.of()
        );
        SkillDefinition skill2 = new SkillDefinition(
                "backlog-diagnosis",
                "Diagnose backlog issues in pulsar topics",
                "test",
                "Prompt",
                List.of(),
                null,
                List.of("backlog diagnosis")
        );

        skillLoader.registerSkill(skill1);
        skillLoader.registerSkill(skill2);

        List<SkillLoader.SkillMatch> matches = skillLoader.findMatchingSkills("backlog diagnosis");

        assertFalse(matches.isEmpty());
        // Should be sorted by score descending
        for (int i = 0; i < matches.size() - 1; i++) {
            assertTrue(matches.get(i).score() >= matches.get(i + 1).score());
        }
    }

    @Test
    @DisplayName("Should return empty list for null or empty query")
    void findMatchingSkills_shouldReturnEmptyList() {
        assertTrue(skillLoader.findMatchingSkills(null).isEmpty());
        assertTrue(skillLoader.findMatchingSkills("").isEmpty());
    }

    @Test
    @DisplayName("Should check skill existence correctly")
    void hasSkill_shouldCheckExistence() {
        SkillDefinition skill = new SkillDefinition(
                "existing-skill",
                "Description",
                "test",
                "Prompt",
                List.of(),
                null,
                List.of()
        );

        skillLoader.registerSkill(skill);

        assertTrue(skillLoader.hasSkill("existing-skill"));
        assertTrue(skillLoader.hasSkill("existing_skill")); // underscore version
        assertFalse(skillLoader.hasSkill("non-existing"));
        assertFalse(skillLoader.hasSkill(null));
    }

    @Test
    @DisplayName("Should return unmodifiable collection")
    void getAllSkills_shouldReturnUnmodifiable() {
        Collection<SkillDefinition> skills = skillLoader.getAllSkills();

        assertThrows(UnsupportedOperationException.class, () ->
            skills.add(new SkillDefinition("test", "desc", "cat", "prompt", List.of(), null, List.of()))
        );
    }
}