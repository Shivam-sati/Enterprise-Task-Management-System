package com.taskmanagement.analytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.analytics.dto.TaskDto;
import com.taskmanagement.analytics.model.*;
import com.taskmanagement.analytics.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@Import(com.taskmanagement.analytics.config.TestSecurityConfig.class)
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyticsCalculator analyticsCalculator;

    @MockBean
    private DataAggregator dataAggregator;

    @MockBean
    private TrendAnalysisEngine trendAnalysisEngine;

    @MockBean
    private ProductivityPredictor productivityPredictor;

    @MockBean
    private ProactiveAlertSystem proactiveAlertSystem;

    @MockBean
    private TaskServiceClient taskServiceClient;

    private String testUserId;
    private List<TaskDto> mockTasks;
    private ProductivityMetrics mockMetrics;
    private TaskStatistics mockStatistics;
    private TrendAnalysis mockTrendAnalysis;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
        setupMockData();
    }

    private void setupMockData() {
        // Mock tasks
        mockTasks = Arrays.asList(
                createMockTask("task1", "COMPLETED", "HIGH"),
                createMockTask("task2", "COMPLETED", "MEDIUM"),
                createMockTask("task3", "TODO", "LOW")
        );

        // Mock productivity metrics
        mockMetrics = ProductivityMetrics.builder()
                .userId(testUserId)
                .completionRate(75.0)
                .averageTaskTime(2.5)
                .tasksCompleted(15)
                .tasksCreated(20)
                .productivityScore(7.5)
                .period(Period.ofDays(30))
                .calculatedAt(LocalDateTime.now())
                .breakdown(Map.of("statusBreakdown", Map.of("COMPLETED", 15, "TODO", 5)))
                .confidence(0.8)
                .dataQuality("HIGH_QUALITY")
                .build();

        // Mock task statistics
        mockStatistics = TaskStatistics.builder()
                .userId(testUserId)
                .totalTasks(20)
                .completedTasks(15)
                .pendingTasks(5)
                .overdueTasks(2)
                .cancelledTasks(0)
                .averageCompletionTime(2.5)
                .totalTimeSpent(37.5)
                .tasksByPriority(Map.of("HIGH", 8, "MEDIUM", 7, "LOW", 5))
                .tasksByCategory(Map.of("Work", 12, "Personal", 8))
                .dailyCounts(Arrays.asList(
                        DailyTaskCount.builder().date(LocalDateTime.now().toLocalDate()).completed(3).created(4).build()
                ))
                .calculatedAt(LocalDateTime.now())
                .build();

        // Mock trend analysis
        mockTrendAnalysis = TrendAnalysis.builder()
                .userId(testUserId)
                .completionTrends(Arrays.asList(
                        CompletionTrend.builder()
                                .date(LocalDateTime.now().toLocalDate())
                                .completed(3)
                                .created(4)
                                .completionRate(75.0)
                                .build()
                ))
                .productivityPattern(ProductivityPattern.builder()
                        .bestHour(10)
                        .bestDay(java.time.DayOfWeek.TUESDAY)
                        .averageSessionTime(45.0)
                        .workingPattern("MORNING_PERSON")
                        .consistency(0.7)
                        .build())
                .insights(Arrays.asList("You're most productive on Tuesdays", "Peak productivity at 10 AM"))
                .performanceMetrics(Map.of("overallCompletionRate", 75.0))
                .analyzedAt(LocalDateTime.now())
                .confidence(0.8)
                .trendDirection("IMPROVING")
                .trendStrength(0.6)
                .build();
    }

    private TaskDto createMockTask(String id, String status, String priority) {
        TaskDto.TaskDtoBuilder builder = TaskDto.builder()
                .taskId(id)
                .userId(testUserId)
                .title("Test Task " + id)
                .description("Test description for " + id)
                .status(status)
                .priority(priority)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .estimatedHours(2.0)
                .actualHours(2.5)
                .tags(Arrays.asList("test", "mock"));
        
        if ("COMPLETED".equals(status)) {
            builder.completedAt(LocalDateTime.now());
        }
        
        return builder.build();
    }

    @Test
    void testGetProductivityMetrics_Success() throws Exception {
        // Arrange
        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenReturn(mockTasks);
        when(analyticsCalculator.calculateProductivity(eq(testUserId), eq(mockTasks), any(Period.class)))
                .thenReturn(mockMetrics);

        // Act & Assert
        mockMvc.perform(get("/api/analytics/productivity/{userId}", testUserId)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.completionRate").value(75.0))
                .andExpect(jsonPath("$.averageTaskTime").value(2.5))
                .andExpect(jsonPath("$.tasksCompleted").value(15))
                .andExpect(jsonPath("$.tasksCreated").value(20))
                .andExpect(jsonPath("$.productivityScore").value(7.5))
                .andExpect(jsonPath("$.confidence").value(0.8))
                .andExpect(jsonPath("$.dataQuality").value("HIGH_QUALITY"))
                .andExpect(jsonPath("$.dataSource").value("REAL_TASK_DATA"))
                .andExpect(jsonPath("$.calculationMethod").value("ANALYTICS_CALCULATOR"))
                .andExpect(jsonPath("$.breakdown").exists());
    }

    @Test
    void testGetProductivityMetrics_InsufficientData() throws Exception {
        // Arrange
        ProductivityMetrics emptyMetrics = ProductivityMetrics.builder()
                .userId(testUserId)
                .completionRate(0.0)
                .averageTaskTime(0.0)
                .tasksCompleted(0)
                .tasksCreated(0)
                .productivityScore(0.0)
                .confidence(0.0)
                .dataQuality("NO_DATA")
                .calculatedAt(LocalDateTime.now())
                .build();

        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenReturn(Collections.emptyList());
        when(analyticsCalculator.calculateProductivity(eq(testUserId), eq(Collections.emptyList()), any(Period.class)))
                .thenReturn(emptyMetrics);

        // Act & Assert
        mockMvc.perform(get("/api/analytics/productivity/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.dataQuality").value("NO_DATA"))
                .andExpect(jsonPath("$.warning").value("Insufficient data for reliable metrics. Complete more tasks to improve accuracy."));
    }

    @Test
    void testGetProductivityMetrics_Error() throws Exception {
        // Arrange
        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Act & Assert
        mockMvc.perform(get("/api/analytics/productivity/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.error").value("Unable to calculate real metrics"))
                .andExpect(jsonPath("$.dataSource").value("ERROR_FALLBACK"))
                .andExpect(jsonPath("$.dataQuality").value("ERROR"));
    }

    @Test
    void testGetDashboardData_Success() throws Exception {
        // Arrange
        when(dataAggregator.aggregateTaskData(eq(testUserId), any(Period.class)))
                .thenReturn(mockStatistics);

        // Act & Assert
        mockMvc.perform(get("/api/analytics/dashboard/{userId}", testUserId)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(20))
                .andExpect(jsonPath("$.completedTasks").value(15))
                .andExpect(jsonPath("$.pendingTasks").value(5))
                .andExpect(jsonPath("$.overdueTasks").value(2))
                .andExpect(jsonPath("$.dataSource").value("REAL_TASK_DATA"))
                .andExpect(jsonPath("$.topCategories").isArray())
                .andExpect(jsonPath("$.priorityBreakdown").exists())
                .andExpect(jsonPath("$.timeSpent").exists());
    }

    @Test
    void testGetDashboardData_NoData() throws Exception {
        // Arrange
        TaskStatistics emptyStats = TaskStatistics.builder()
                .userId(testUserId)
                .totalTasks(0)
                .completedTasks(0)
                .pendingTasks(0)
                .overdueTasks(0)
                .calculatedAt(LocalDateTime.now())
                .build();

        when(dataAggregator.aggregateTaskData(eq(testUserId), any(Period.class)))
                .thenReturn(emptyStats);

        // Act & Assert
        mockMvc.perform(get("/api/analytics/dashboard/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(0))
                .andExpect(jsonPath("$.warning").value("No tasks found for the specified period"))
                .andExpect(jsonPath("$.dataQuality").value("NO_DATA"));
    }

    @Test
    void testGetTrends_Success() throws Exception {
        // Arrange
        when(trendAnalysisEngine.analyzeTrends(eq(testUserId), any(Period.class)))
                .thenReturn(mockTrendAnalysis);

        // Act & Assert
        mockMvc.perform(get("/api/analytics/trends/{userId}", testUserId)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionTrend").isArray())
                .andExpect(jsonPath("$.productivityPattern.bestHour").value(10))
                .andExpect(jsonPath("$.productivityPattern.bestDay").value("TUESDAY"))
                .andExpect(jsonPath("$.insights").isArray())
                .andExpect(jsonPath("$.confidence").value(0.8))
                .andExpect(jsonPath("$.trendDirection").value("IMPROVING"))
                .andExpect(jsonPath("$.trendStrength").value(0.6))
                .andExpect(jsonPath("$.dataSource").value("REAL_TASK_DATA"));
    }

    @Test
    void testGetTrends_LowConfidence() throws Exception {
        // Arrange
        TrendAnalysis lowConfidenceTrend = TrendAnalysis.builder()
                .userId(testUserId)
                .completionTrends(Collections.emptyList())
                .productivityPattern(ProductivityPattern.builder().build())
                .insights(Arrays.asList("Insufficient data"))
                .confidence(0.3)
                .trendDirection("INSUFFICIENT_DATA")
                .analyzedAt(LocalDateTime.now())
                .build();

        when(trendAnalysisEngine.analyzeTrends(eq(testUserId), any(Period.class)))
                .thenReturn(lowConfidenceTrend);

        // Act & Assert
        mockMvc.perform(get("/api/analytics/trends/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confidence").value(0.3))
                .andExpect(jsonPath("$.warning").value("Low confidence in trend analysis due to insufficient data"))
                .andExpect(jsonPath("$.dataQuality").value("LOW_CONFIDENCE"));
    }

    @Test
    void testGetProductivityForecast_Success() throws Exception {
        // Arrange
        ProductivityForecast mockForecast = ProductivityForecast.builder()
                .userId(testUserId)
                .overallForecastScore(7.2)
                .confidence(0.75)
                .forecastMethod("TREND_BASED_WITH_PATTERNS")
                .generatedAt(LocalDateTime.now())
                .forecastStartDate(LocalDateTime.now().plusDays(1).toLocalDate())
                .forecastEndDate(LocalDateTime.now().plusDays(7).toLocalDate())
                .uncertaintyRange(1.5)
                .dailyPredictions(Arrays.asList(
                        DailyProductivityPrediction.builder()
                                .date(LocalDateTime.now().plusDays(1).toLocalDate())
                                .predictedScore(7.5)
                                .confidence(0.8)
                                .lowerBound(6.0)
                                .upperBound(9.0)
                                .expectedTasksCompleted(3)
                                .expectedCompletionRate(85.0)
                                .dayOfWeekPattern("MID_WEEK")
                                .reasoning("Based on Tuesday patterns")
                                .build()
                ))
                .metadata(Map.of("algorithm", "PATTERN_BASED"))
                .assumptions(Arrays.asList("Historical patterns continue"))
                .build();

        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenReturn(mockTasks);
        when(productivityPredictor.predictProductivity(eq(testUserId), eq(mockTasks), eq(7)))
                .thenReturn(mockForecast);

        // Act & Assert
        mockMvc.perform(get("/api/analytics/forecast/{userId}", testUserId)
                        .param("forecastDays", "7")
                        .param("historicalDays", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.overallForecastScore").value(7.2))
                .andExpect(jsonPath("$.confidence").value(0.75))
                .andExpect(jsonPath("$.forecastMethod").value("TREND_BASED_WITH_PATTERNS"))
                .andExpect(jsonPath("$.dailyPredictions").isArray())
                .andExpect(jsonPath("$.dailyPredictions[0].predictedScore").value(7.5))
                .andExpect(jsonPath("$.dataSource").value("PREDICTIVE_ANALYTICS"));
    }

    @Test
    void testGetProactiveAlerts_Success() throws Exception {
        // Arrange
        List<ProactiveAlert> mockAlerts = Arrays.asList(
                ProactiveAlert.builder()
                        .alertId("alert1")
                        .userId(testUserId)
                        .type(ProactiveAlert.AlertType.PRODUCTIVITY_DROP)
                        .severity(ProactiveAlert.AlertSeverity.MEDIUM)
                        .title("Productivity Drop Predicted")
                        .message("Predicted productivity drop in upcoming days")
                        .recommendation("Review task priorities")
                        .confidence(0.7)
                        .triggeredAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .acknowledged(false)
                        .context(Map.of("affectedDays", 3))
                        .actionItems(Arrays.asList("Review priorities", "Break down tasks"))
                        .triggerReason("Forecast shows low productivity")
                        .build()
        );

        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenReturn(mockTasks);
        when(proactiveAlertSystem.generateProactiveAlerts(eq(testUserId), eq(mockTasks), any()))
                .thenReturn(mockAlerts);

        // Act & Assert
        mockMvc.perform(get("/api/analytics/alerts/{userId}", testUserId)
                        .param("sensitivity", "MEDIUM")
                        .param("historicalDays", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.alertCount").value(1))
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.alerts[0].alertId").value("alert1"))
                .andExpect(jsonPath("$.alerts[0].type").value("PRODUCTIVITY_DROP"))
                .andExpect(jsonPath("$.alerts[0].severity").value("MEDIUM"))
                .andExpect(jsonPath("$.severityBreakdown.MEDIUM").value(1))
                .andExpect(jsonPath("$.dataSource").value("PROACTIVE_ALERT_SYSTEM"));
    }

    @Test
    void testGetProactiveAlerts_NoAlerts() throws Exception {
        // Arrange
        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenReturn(mockTasks);
        when(proactiveAlertSystem.generateProactiveAlerts(eq(testUserId), eq(mockTasks), any()))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/analytics/alerts/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertCount").value(0))
                .andExpect(jsonPath("$.alerts").isEmpty())
                .andExpect(jsonPath("$.message").value("No alerts generated - productivity patterns appear normal"))
                .andExpect(jsonPath("$.dataQuality").value("NORMAL"));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/analytics/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Analytics Service"));
    }

    @Test
    void testPerformanceWithLargeDataset() throws Exception {
        // Arrange - simulate large dataset
        List<TaskDto> largeMockTasks = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeMockTasks.add(createMockTask("task" + i, "COMPLETED", "MEDIUM"));
        }

        when(taskServiceClient.getTasksForUser(eq(testUserId), any(Period.class)))
                .thenReturn(largeMockTasks);
        when(analyticsCalculator.calculateProductivity(eq(testUserId), eq(largeMockTasks), any(Period.class)))
                .thenReturn(mockMetrics);

        // Act & Assert - should complete within reasonable time
        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/analytics/productivity/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUserId));
        
        long duration = System.currentTimeMillis() - startTime;
        // Assert that response time is reasonable (less than 5 seconds for test)
        assert duration < 5000 : "Response took too long: " + duration + "ms";
    }

    @Test
    void testParameterValidation() throws Exception {
        // Test with invalid parameters
        mockMvc.perform(get("/api/analytics/productivity/{userId}", testUserId)
                        .param("days", "-1"))
                .andExpect(status().isOk()); // Should handle gracefully

        mockMvc.perform(get("/api/analytics/forecast/{userId}", testUserId)
                        .param("forecastDays", "0")
                        .param("historicalDays", "0"))
                .andExpect(status().isOk()); // Should handle gracefully
    }
}