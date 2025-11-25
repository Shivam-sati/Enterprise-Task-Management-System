package com.taskmanagement.analytics.service;

import com.taskmanagement.analytics.dto.TaskDto;
import com.taskmanagement.analytics.model.ProductivityMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsCalculatorTest {

    @InjectMocks
    private AnalyticsCalculator analyticsCalculator;

    private List<TaskDto> testTasks;
    private String testUserId;
    private Period testPeriod;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
        testPeriod = Period.ofDays(30);
        testTasks = createTestTasks();
    }

    @Test
    void testCalculateProductivity_WithValidTasks() {
        // Given
        List<TaskDto> tasks = createCompletedTasks(10, 8);

        // When
        ProductivityMetrics metrics = analyticsCalculator.calculateProductivity(testUserId, tasks, testPeriod);

        // Then
        assertNotNull(metrics);
        assertEquals(testUserId, metrics.getUserId());
        assertEquals(80.0, metrics.getCompletionRate(), 0.1);
        assertEquals(8, metrics.getTasksCompleted());
        assertEquals(10, metrics.getTasksCreated());
        assertTrue(metrics.getProductivityScore() > 0);
        assertNotNull(metrics.getBreakdown());
        assertTrue(metrics.getConfidence() > 0);
    }

    @Test
    void testCalculateProductivity_WithEmptyTasks() {
        // Given
        List<TaskDto> emptyTasks = new ArrayList<>();

        // When
        ProductivityMetrics metrics = analyticsCalculator.calculateProductivity(testUserId, emptyTasks, testPeriod);

        // Then
        assertNotNull(metrics);
        assertEquals(testUserId, metrics.getUserId());
        assertEquals(0.0, metrics.getCompletionRate());
        assertEquals(0, metrics.getTasksCompleted());
        assertEquals(0, metrics.getTasksCreated());
        assertEquals(0.0, metrics.getProductivityScore());
        assertEquals("NO_DATA", metrics.getDataQuality());
        assertEquals(0.0, metrics.getConfidence());
    }

    @Test
    void testCalculateProductivity_WithHighPriorityTasks() {
        // Given
        List<TaskDto> tasks = createHighPriorityTasks(5, 4);

        // When
        ProductivityMetrics metrics = analyticsCalculator.calculateProductivity(testUserId, tasks, testPeriod);

        // Then
        assertNotNull(metrics);
        assertTrue(metrics.getProductivityScore() > 0.0); // Should have some score
        assertEquals(80.0, metrics.getCompletionRate(), 0.1);
    }

    @Test
    void testCalculateProductivity_WithInsufficientData() {
        // Given
        List<TaskDto> fewTasks = createCompletedTasks(3, 2);

        // When
        ProductivityMetrics metrics = analyticsCalculator.calculateProductivity(testUserId, fewTasks, testPeriod);

        // Then
        assertNotNull(metrics);
        assertEquals("INSUFFICIENT_DATA", metrics.getDataQuality());
        assertTrue(metrics.getConfidence() < 0.5);
    }

    @Test
    void testCalculateProductivity_WithRealisticTimeData() {
        // Given
        List<TaskDto> tasks = createTasksWithTimeData();

        // When
        ProductivityMetrics metrics = analyticsCalculator.calculateProductivity(testUserId, tasks, testPeriod);

        // Then
        assertNotNull(metrics);
        assertTrue(metrics.getAverageTaskTime() > 0);
        assertTrue(metrics.getAverageTaskTime() < 168); // Less than a week
        assertNotNull(metrics.getBreakdown().get("timeAnalysis"));
    }

    @Test
    void testCalculateProductivity_EdgeCases() {
        // Test with tasks having null completion dates
        List<TaskDto> tasksWithNulls = createTasksWithNullDates();
        ProductivityMetrics metrics = analyticsCalculator.calculateProductivity(testUserId, tasksWithNulls, testPeriod);
        
        assertNotNull(metrics);
        assertTrue(metrics.getCompletionRate() >= 0);
        
        // Test with unrealistic time data
        List<TaskDto> tasksWithBadTime = createTasksWithUnrealisticTime();
        metrics = analyticsCalculator.calculateProductivity(testUserId, tasksWithBadTime, testPeriod);
        
        assertNotNull(metrics);
        assertTrue(metrics.getAverageTaskTime() >= 0); // Can be 0 if no valid time data
    }

    private List<TaskDto> createTestTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 10; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("task-" + i)
                    .userId(testUserId)
                    .title("Test Task " + i)
                    .status(i < 7 ? "COMPLETED" : "TODO")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(i + 1))
                    .completedAt(i < 7 ? now.minusDays(i) : null)
                    .estimatedHours(2.0)
                    .actualHours(i < 7 ? 2.5 : null)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createCompletedTasks(int total, int completed) {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < total; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("task-" + i)
                    .userId(testUserId)
                    .title("Test Task " + i)
                    .status(i < completed ? "COMPLETED" : "TODO")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(i + 1))
                    .completedAt(i < completed ? now.minusDays(i) : null)
                    .estimatedHours(2.0)
                    .actualHours(i < completed ? 2.0 : null)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createHighPriorityTasks(int total, int completed) {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < total; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("task-" + i)
                    .userId(testUserId)
                    .title("High Priority Task " + i)
                    .status(i < completed ? "COMPLETED" : "TODO")
                    .priority("HIGH")
                    .createdAt(now.minusDays(i + 1))
                    .completedAt(i < completed ? now.minusDays(i) : null)
                    .estimatedHours(3.0)
                    .actualHours(i < completed ? 3.0 : null)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createTasksWithTimeData() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 15; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("task-" + i)
                    .userId(testUserId)
                    .title("Timed Task " + i)
                    .status(i < 12 ? "COMPLETED" : "TODO")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(i + 1))
                    .completedAt(i < 12 ? now.minusDays(i).plusHours(i % 8 + 1) : null)
                    .estimatedHours((double) (i % 5 + 1))
                    .actualHours(i < 12 ? (double) (i % 6 + 1) : null)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createTasksWithNullDates() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 8; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("task-" + i)
                    .userId(testUserId)
                    .title("Task with Nulls " + i)
                    .status(i < 5 ? "COMPLETED" : "TODO")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(i + 1))
                    .completedAt(i < 5 && i % 2 == 0 ? now.minusDays(i) : null) // Some null completion dates
                    .estimatedHours(2.0)
                    .actualHours(i < 5 ? 2.0 : null)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createTasksWithUnrealisticTime() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 6; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("task-" + i)
                    .userId(testUserId)
                    .title("Bad Time Task " + i)
                    .status("COMPLETED")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(200)) // Very old creation date
                    .completedAt(now.minusDays(i))
                    .estimatedHours(2.0)
                    .actualHours(2.0)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }
}