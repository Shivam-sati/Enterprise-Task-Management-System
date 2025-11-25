package com.taskmanagement.analytics.service;

import com.taskmanagement.analytics.dto.TaskDto;
import com.taskmanagement.analytics.model.TaskStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataAggregatorTest {

    @Mock
    private TaskServiceClient taskServiceClient;

    @InjectMocks
    private DataAggregator dataAggregator;

    private String testUserId;
    private Period testPeriod;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
        testPeriod = Period.ofDays(30);
    }

    @Test
    void testAggregateTaskData_WithValidTasks() {
        // Given
        List<TaskDto> mockTasks = createMockTasks();
        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenReturn(mockTasks);

        // When
        TaskStatistics statistics = dataAggregator.aggregateTaskData(testUserId, testPeriod);

        // Then
        assertNotNull(statistics);
        assertEquals(testUserId, statistics.getUserId());
        assertEquals(10, statistics.getTotalTasks());
        assertEquals(7, statistics.getCompletedTasks());
        assertEquals(2, statistics.getPendingTasks());
        assertEquals(1, statistics.getCancelledTasks());
        assertTrue(statistics.getAverageCompletionTime() > 0);
        assertNotNull(statistics.getTasksByPriority());
        assertNotNull(statistics.getTasksByCategory());
        assertNotNull(statistics.getDailyCounts());
    }

    @Test
    void testAggregateTaskData_WithEmptyTasks() {
        // Given
        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenReturn(new ArrayList<>());

        // When
        TaskStatistics statistics = dataAggregator.aggregateTaskData(testUserId, testPeriod);

        // Then
        assertNotNull(statistics);
        assertEquals(testUserId, statistics.getUserId());
        assertEquals(0, statistics.getTotalTasks());
        assertEquals(0, statistics.getCompletedTasks());
        assertEquals(0, statistics.getPendingTasks());
        assertEquals(0, statistics.getOverdueTasks());
        assertEquals(0.0, statistics.getAverageCompletionTime());
        assertEquals(0.0, statistics.getTotalTimeSpent());
    }

    @Test
    void testAggregateTaskData_WithOverdueTasks() {
        // Given
        List<TaskDto> tasksWithOverdue = createTasksWithOverdue();
        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenReturn(tasksWithOverdue);

        // When
        TaskStatistics statistics = dataAggregator.aggregateTaskData(testUserId, testPeriod);

        // Then
        assertNotNull(statistics);
        assertTrue(statistics.getOverdueTasks() > 0);
    }

    @Test
    void testAnalyzeTimePatterns() {
        // Given
        List<TaskDto> mockTasks = createTasksWithTimePatterns();
        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenReturn(mockTasks);

        // When
        Map<String, Double> patterns = dataAggregator.analyzeTimePatterns(testUserId, testPeriod);

        // Then
        assertNotNull(patterns);
        assertTrue(patterns.containsKey("bestDayOfWeek") || patterns.containsKey("bestHour"));
    }

    @Test
    void testAnalyzeCategoryDistribution() {
        // Given
        List<TaskDto> mockTasks = createTasksWithCategories();
        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenReturn(mockTasks);

        // When
        Map<String, Integer> distribution = dataAggregator.analyzeCategoryDistribution(testUserId, testPeriod);

        // Then
        assertNotNull(distribution);
        assertTrue(distribution.containsKey("Work") || distribution.containsKey("Personal"));
    }

    @Test
    void testAggregateTaskData_HandleException() {
        // Given
        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When
        TaskStatistics statistics = dataAggregator.aggregateTaskData(testUserId, testPeriod);

        // Then
        assertNotNull(statistics);
        assertEquals(testUserId, statistics.getUserId());
        assertEquals(0, statistics.getTotalTasks());
    }

    @Test
    void testDailyCountsCalculation() {
        // Given
        List<TaskDto> tasksSpreadOverDays = createTasksSpreadOverDays();
        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenReturn(tasksSpreadOverDays);

        // When
        TaskStatistics statistics = dataAggregator.aggregateTaskData(testUserId, testPeriod);

        // Then
        assertNotNull(statistics.getDailyCounts());
        assertFalse(statistics.getDailyCounts().isEmpty());
        
        // Verify daily counts are properly calculated
        boolean hasCreatedTasks = statistics.getDailyCounts().stream()
                .anyMatch(dc -> dc.getCreated() > 0);
        boolean hasCompletedTasks = statistics.getDailyCounts().stream()
                .anyMatch(dc -> dc.getCompleted() > 0);
        
        assertTrue(hasCreatedTasks);
        assertTrue(hasCompletedTasks);
    }

    private List<TaskDto> createMockTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create various task statuses
        String[] statuses = {"COMPLETED", "COMPLETED", "COMPLETED", "COMPLETED", "COMPLETED", 
                           "COMPLETED", "COMPLETED", "TODO", "IN_PROGRESS", "CANCELLED"};
        String[] priorities = {"HIGH", "MEDIUM", "LOW", "HIGH", "MEDIUM", 
                             "LOW", "CRITICAL", "MEDIUM", "HIGH", "LOW"};
        
        for (int i = 0; i < 10; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("task-" + i)
                    .userId(testUserId)
                    .title("Test Task " + i)
                    .status(statuses[i])
                    .priority(priorities[i])
                    .createdAt(now.minusDays(i + 1))
                    .completedAt("COMPLETED".equals(statuses[i]) ? now.minusDays(i) : null)
                    .estimatedHours(2.0)
                    .actualHours("COMPLETED".equals(statuses[i]) ? 2.5 : null)
                    .tags(List.of("Work", "Important"))
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createTasksWithOverdue() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 5; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("overdue-task-" + i)
                    .userId(testUserId)
                    .title("Overdue Task " + i)
                    .status(i < 2 ? "TODO" : "COMPLETED")
                    .priority("HIGH")
                    .createdAt(now.minusDays(10))
                    .dueDate(now.minusDays(2)) // Overdue
                    .completedAt(i >= 2 ? now.minusDays(1) : null)
                    .estimatedHours(3.0)
                    .actualHours(i >= 2 ? 3.5 : null)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createTasksWithTimePatterns() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 8; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("pattern-task-" + i)
                    .userId(testUserId)
                    .title("Pattern Task " + i)
                    .status("COMPLETED")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(i + 1))
                    .completedAt(now.minusDays(i).withHour(9 + (i % 3))) // Different completion hours
                    .estimatedHours(2.0)
                    .actualHours(2.0)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createTasksWithCategories() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        String[] categories = {"Work", "Personal", "Learning", "Work", "Personal"};
        
        for (int i = 0; i < 5; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("category-task-" + i)
                    .userId(testUserId)
                    .title("Category Task " + i)
                    .status("COMPLETED")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(i + 1))
                    .completedAt(now.minusDays(i))
                    .tags(List.of(categories[i]))
                    .estimatedHours(2.0)
                    .actualHours(2.0)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createTasksSpreadOverDays() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 14; i++) {
            // Create tasks
            TaskDto createdTask = TaskDto.builder()
                    .taskId("spread-task-created-" + i)
                    .userId(testUserId)
                    .title("Spread Task Created " + i)
                    .status(i < 10 ? "COMPLETED" : "TODO")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(i))
                    .completedAt(i < 10 ? now.minusDays(i - 1) : null)
                    .estimatedHours(2.0)
                    .actualHours(i < 10 ? 2.0 : null)
                    .build();
            tasks.add(createdTask);
        }
        
        return tasks;
    }
}