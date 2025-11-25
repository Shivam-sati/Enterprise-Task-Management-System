package com.taskmanagement.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FallbackServiceTest {

    private FallbackService fallbackService;

    @BeforeEach
    void setUp() {
        fallbackService = new FallbackService();
        ReflectionTestUtils.setField(fallbackService, "fallbackEnabled", true);
    }

    @Test
    void parseTaskFallback_WithValidText_ShouldReturnFallbackResponse() {
        // Arrange
        Map<String, String> request = Map.of("text", "Create a new user authentication system");

        // Act
        Map<String, Object> result = fallbackService.parseTaskFallback(request);

        // Assert
        assertNotNull(result);
        assertEquals("Create a new user authentication system", result.get("title"));
        assertEquals("Create a new user authentication system", result.get("description"));
        assertEquals("MEDIUM", result.get("priority"));
        assertEquals(8, result.get("estimatedHours")); // Long text should get 8 hours
        assertEquals("fallback", result.get("source"));
        assertEquals(0.5, result.get("confidence"));
        assertTrue(result.get("message").toString().contains("Python AI service unavailable"));
        assertNotNull(result.get("timestamp"));
        assertTrue((Long) result.get("fallbackCount") > 0);
    }

    @Test
    void parseTaskFallback_WithUrgentText_ShouldReturnHighPriority() {
        // Arrange
        Map<String, String> request = Map.of("text", "URGENT: Fix critical security vulnerability");

        // Act
        Map<String, Object> result = fallbackService.parseTaskFallback(request);

        // Assert
        assertEquals("HIGH", result.get("priority"));
    }

    @Test
    void parseTaskFallback_WithLaterText_ShouldReturnLowPriority() {
        // Arrange
        Map<String, String> request = Map.of("text", "Maybe later we can improve the UI");

        // Act
        Map<String, Object> result = fallbackService.parseTaskFallback(request);

        // Assert
        assertEquals("LOW", result.get("priority"));
    }

    @Test
    void parseTaskFallback_WithShortText_ShouldReturnLowEstimate() {
        // Arrange
        Map<String, String> request = Map.of("text", "Fix bug");

        // Act
        Map<String, Object> result = fallbackService.parseTaskFallback(request);

        // Assert
        assertEquals(1, result.get("estimatedHours"));
    }

    @Test
    void parseTaskFallback_WithEmptyText_ShouldReturnDefaultValues() {
        // Arrange
        Map<String, String> request = Map.of("text", "");

        // Act
        Map<String, Object> result = fallbackService.parseTaskFallback(request);

        // Assert
        assertEquals("Untitled Task (Fallback)", result.get("title"));
        assertEquals("No description provided", result.get("description"));
        assertEquals("MEDIUM", result.get("priority"));
    }

    @Test
    void parseTaskFallback_WithNullText_ShouldReturnDefaultValues() {
        // Arrange
        Map<String, String> request = Map.of();

        // Act
        Map<String, Object> result = fallbackService.parseTaskFallback(request);

        // Assert
        assertEquals("Untitled Task (Fallback)", result.get("title"));
        assertEquals("No description provided", result.get("description"));
        assertEquals("MEDIUM", result.get("priority"));
    }

    @Test
    void prioritizeTasksFallback_WithTasks_ShouldReturnFallbackResponse() {
        // Arrange
        Map<String, Object> request = Map.of("tasks", List.of("task1", "task2", "task3"));

        // Act
        Map<String, Object> result = fallbackService.prioritizeTasksFallback(request);

        // Assert
        assertNotNull(result);
        String[] prioritizedTasks = (String[]) result.get("prioritizedTasks");
        assertEquals(3, prioritizedTasks.length);
        assertEquals("task1", prioritizedTasks[0]);
        assertEquals("fallback", result.get("source"));
        assertEquals(0.3, result.get("confidence"));
        assertTrue(result.get("reasoning").toString().contains("Python AI service unavailable"));
        assertNotNull(result.get("timestamp"));
    }

    @Test
    void prioritizeTasksFallback_WithNoTasks_ShouldReturnDefaultResponse() {
        // Arrange
        Map<String, Object> request = Map.of();

        // Act
        Map<String, Object> result = fallbackService.prioritizeTasksFallback(request);

        // Assert
        String[] prioritizedTasks = (String[]) result.get("prioritizedTasks");
        assertEquals(1, prioritizedTasks.length);
        assertEquals("No tasks provided", prioritizedTasks[0]);
    }

    @Test
    void getInsightsFallback_ShouldReturnGenericInsights() {
        // Act
        Map<String, Object> result = fallbackService.getInsightsFallback();

        // Assert
        assertNotNull(result);
        String[] insights = (String[]) result.get("insights");
        String[] recommendations = (String[]) result.get("recommendations");
        
        assertTrue(insights.length > 0);
        assertTrue(recommendations.length > 0);
        assertEquals("fallback", result.get("source"));
        assertEquals(0.2, result.get("confidence"));
        assertTrue(result.get("message").toString().contains("Python AI service unavailable"));
        assertNotNull(result.get("timestamp"));
        
        // Check that all insights and recommendations are marked as fallback
        for (String insight : insights) {
            assertTrue(insight.contains("⚠️") || insight.contains("fallback"));
        }
        for (String recommendation : recommendations) {
            assertTrue(recommendation.contains("⚠️") || recommendation.contains("fallback"));
        }
    }

    @Test
    void fallbackCounter_ShouldIncrementWithEachCall() {
        // Arrange
        long initialCount = fallbackService.getFallbackUsageCount();

        // Act
        fallbackService.parseTaskFallback(Map.of("text", "test"));
        fallbackService.prioritizeTasksFallback(Map.of());
        fallbackService.getInsightsFallback();

        // Assert
        assertEquals(initialCount + 3, fallbackService.getFallbackUsageCount());
    }

    @Test
    void resetFallbackCounter_ShouldResetToZero() {
        // Arrange
        fallbackService.parseTaskFallback(Map.of("text", "test"));
        assertTrue(fallbackService.getFallbackUsageCount() > 0);

        // Act
        fallbackService.resetFallbackCounter();

        // Assert
        assertEquals(0, fallbackService.getFallbackUsageCount());
    }

    @Test
    void parseTaskFallback_WhenFallbackDisabled_ShouldThrowException() {
        // Arrange
        ReflectionTestUtils.setField(fallbackService, "fallbackEnabled", false);
        Map<String, String> request = Map.of("text", "test");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> fallbackService.parseTaskFallback(request));
        assertTrue(exception.getMessage().contains("Fallback service is disabled"));
    }
}