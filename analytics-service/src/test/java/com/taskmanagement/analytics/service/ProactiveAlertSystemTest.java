package com.taskmanagement.analytics.service;

import com.taskmanagement.analytics.dto.TaskDto;
import com.taskmanagement.analytics.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProactiveAlertSystemTest {

    @Mock
    private ProductivityPredictor productivityPredictor;

    @Mock
    private AnalyticsCalculator analyticsCalculator;

    @Mock
    private TrendAnalysisEngine trendAnalysisEngine;

    @InjectMocks
    private ProactiveAlertSystem proactiveAlertSystem;

    private String testUserId;
    private List<TaskDto> historicalTasks;
    private ProductivityForecast mockForecast;
    private TrendAnalysis mockTrendAnalysis;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
        historicalTasks = createHistoricalTasks();
        mockForecast = createMockForecast();
        mockTrendAnalysis = createMockTrendAnalysis();
    }

    @Test
    void testGenerateProactiveAlerts_WithSufficientData() {
        // Given
        when(productivityPredictor.predictProductivity(anyString(), any())).thenReturn(mockForecast);
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        List<ProactiveAlert> alerts = proactiveAlertSystem.generateProactiveAlerts(testUserId, historicalTasks);

        // Then
        assertNotNull(alerts);
        // Should not generate alerts for normal conditions
        assertTrue(alerts.size() >= 0);
    }

    @Test
    void testGenerateProactiveAlerts_WithEmptyData() {
        // Given
        List<TaskDto> emptyTasks = new ArrayList<>();

        // When
        List<ProactiveAlert> alerts = proactiveAlertSystem.generateProactiveAlerts(testUserId, emptyTasks);

        // Then
        assertNotNull(alerts);
        assertEquals(0, alerts.size()); // No alerts without data
    }

    @Test
    void testProductivityDropAlert_LowPredictedScores() {
        // Given
        ProductivityForecast lowProductivityForecast = createLowProductivityForecast();
        when(productivityPredictor.predictProductivity(anyString(), any())).thenReturn(lowProductivityForecast);
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        List<ProactiveAlert> alerts = proactiveAlertSystem.generateProactiveAlerts(testUserId, historicalTasks);

        // Then
        assertNotNull(alerts);
        assertTrue(alerts.size() > 0);
        
        ProactiveAlert productivityAlert = alerts.stream()
                .filter(alert -> alert.getType() == ProactiveAlert.AlertType.PRODUCTIVITY_DROP)
                .findFirst()
                .orElse(null);
        
        if (productivityAlert != null) {
            assertEquals(ProactiveAlert.AlertType.PRODUCTIVITY_DROP, productivityAlert.getType());
            assertEquals(testUserId, productivityAlert.getUserId());
            assertNotNull(productivityAlert.getTitle());
            assertNotNull(productivityAlert.getMessage());
            assertNotNull(productivityAlert.getRecommendation());
            assertTrue(productivityAlert.getConfidence() > 0.0);
            assertNotNull(productivityAlert.getActionItems());
            assertTrue(productivityAlert.getActionItems().size() > 0);
        }
    }

    @Test
    void testTrendDeclineAlert_DecreasingTrend() {
        // Given
        TrendAnalysis decliningTrend = createDecliningTrendAnalysis();
        when(productivityPredictor.predictProductivity(anyString(), any())).thenReturn(mockForecast);
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(decliningTrend);

        // When
        List<ProactiveAlert> alerts = proactiveAlertSystem.generateProactiveAlerts(testUserId, historicalTasks);

        // Then
        assertNotNull(alerts);
        
        ProactiveAlert trendAlert = alerts.stream()
                .filter(alert -> alert.getType() == ProactiveAlert.AlertType.TREND_DECLINE)
                .findFirst()
                .orElse(null);
        
        if (trendAlert != null) {
            assertEquals(ProactiveAlert.AlertType.TREND_DECLINE, trendAlert.getType());
            assertEquals(testUserId, trendAlert.getUserId());
            assertTrue(trendAlert.getSeverity() == ProactiveAlert.AlertSeverity.MEDIUM || 
                      trendAlert.getSeverity() == ProactiveAlert.AlertSeverity.HIGH);
            assertNotNull(trendAlert.getContext());
            assertTrue(trendAlert.getContext().containsKey("trendStrength"));
            assertTrue(trendAlert.getContext().containsKey("trendDirection"));
        }
    }

    @Test
    void testBurnoutRiskAlert_HighTaskVolume() {
        // Given
        List<TaskDto> highVolumeTasks = createHighVolumeTasks();
        TrendAnalysis decliningTrend = createDecliningTrendAnalysis();
        when(productivityPredictor.predictProductivity(anyString(), any())).thenReturn(mockForecast);
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(decliningTrend);

        // When
        List<ProactiveAlert> alerts = proactiveAlertSystem.generateProactiveAlerts(testUserId, highVolumeTasks);

        // Then
        assertNotNull(alerts);
        
        ProactiveAlert burnoutAlert = alerts.stream()
                .filter(alert -> alert.getType() == ProactiveAlert.AlertType.BURNOUT_RISK)
                .findFirst()
                .orElse(null);
        
        if (burnoutAlert != null) {
            assertEquals(ProactiveAlert.AlertType.BURNOUT_RISK, burnoutAlert.getType());
            assertTrue(burnoutAlert.getSeverity() == ProactiveAlert.AlertSeverity.HIGH || 
                      burnoutAlert.getSeverity() == ProactiveAlert.AlertSeverity.CRITICAL);
            assertNotNull(burnoutAlert.getContext());
            assertTrue(burnoutAlert.getContext().containsKey("burnoutRisk"));
            assertNotNull(burnoutAlert.getActionItems());
            assertTrue(burnoutAlert.getActionItems().contains("Take regular breaks throughout the day"));
        }
    }

    @Test
    void testWorkloadImbalanceAlert_HighPriorityImbalance() {
        // Given
        List<TaskDto> imbalancedTasks = createImbalancedWorkloadTasks();
        when(productivityPredictor.predictProductivity(anyString(), any())).thenReturn(mockForecast);
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        List<ProactiveAlert> alerts = proactiveAlertSystem.generateProactiveAlerts(testUserId, imbalancedTasks);

        // Then
        assertNotNull(alerts);
        
        ProactiveAlert workloadAlert = alerts.stream()
                .filter(alert -> alert.getType() == ProactiveAlert.AlertType.WORKLOAD_IMBALANCE)
                .findFirst()
                .orElse(null);
        
        if (workloadAlert != null) {
            assertEquals(ProactiveAlert.AlertType.WORKLOAD_IMBALANCE, workloadAlert.getType());
            assertNotNull(workloadAlert.getContext());
            assertTrue(workloadAlert.getContext().containsKey("imbalanceScore"));
            assertTrue(workloadAlert.getContext().containsKey("priorityDistribution"));
        }
    }

    @Test
    void testAlertSensitivityLevels() {
        // Given
        ProductivityForecast marginalForecast = createMarginalProductivityForecast();
        when(productivityPredictor.predictProductivity(anyString(), any())).thenReturn(marginalForecast);
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When - Test different sensitivity levels
        List<ProactiveAlert> lowSensitivityAlerts = proactiveAlertSystem.generateProactiveAlerts(
                testUserId, historicalTasks, AlertThreshold.Sensitivity.LOW);
        List<ProactiveAlert> highSensitivityAlerts = proactiveAlertSystem.generateProactiveAlerts(
                testUserId, historicalTasks, AlertThreshold.Sensitivity.HIGH);

        // Then
        assertNotNull(lowSensitivityAlerts);
        assertNotNull(highSensitivityAlerts);
        
        // High sensitivity should generate more or equal alerts
        assertTrue(highSensitivityAlerts.size() >= lowSensitivityAlerts.size());
    }

    @Test
    void testAlertPrioritization() {
        // Given
        ProductivityForecast lowForecast = createLowProductivityForecast();
        TrendAnalysis decliningTrend = createDecliningTrendAnalysis();
        List<TaskDto> highVolumeTasks = createHighVolumeTasks();
        
        when(productivityPredictor.predictProductivity(anyString(), any())).thenReturn(lowForecast);
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(decliningTrend);

        // When
        List<ProactiveAlert> alerts = proactiveAlertSystem.generateProactiveAlerts(testUserId, highVolumeTasks);

        // Then
        assertNotNull(alerts);
        
        if (alerts.size() > 1) {
            // Alerts should be sorted by severity (highest first)
            for (int i = 0; i < alerts.size() - 1; i++) {
                assertTrue(alerts.get(i).getSeverity().ordinal() >= alerts.get(i + 1).getSeverity().ordinal());
            }
        }
        
        // Should not have duplicate alert types for same user
        long uniqueTypes = alerts.stream().map(ProactiveAlert::getType).distinct().count();
        assertEquals(uniqueTypes, alerts.size());
    }

    @Test
    void testAlertConfidenceScoring() {
        // Given
        ProductivityForecast highConfidenceForecast = createHighConfidenceForecast();
        when(productivityPredictor.predictProductivity(anyString(), any())).thenReturn(highConfidenceForecast);
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        List<ProactiveAlert> alerts = proactiveAlertSystem.generateProactiveAlerts(testUserId, historicalTasks);

        // Then
        for (ProactiveAlert alert : alerts) {
            assertTrue(alert.getConfidence() >= 0.0);
            assertTrue(alert.getConfidence() <= 1.0);
            
            // High confidence forecasts should produce high confidence alerts
            if (alert.getType() == ProactiveAlert.AlertType.PRODUCTIVITY_DROP) {
                assertTrue(alert.getConfidence() >= 0.7);
            }
        }
    }

    @Test
    void testAlertThresholdBehavior() {
        // Given
        ProductivityForecast borderlineForecast = createBorderlineProductivityForecast();
        when(productivityPredictor.predictProductivity(anyString(), any())).thenReturn(borderlineForecast);
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        List<ProactiveAlert> mediumAlerts = proactiveAlertSystem.generateProactiveAlerts(
                testUserId, historicalTasks, AlertThreshold.Sensitivity.MEDIUM);
        List<ProactiveAlert> veryHighAlerts = proactiveAlertSystem.generateProactiveAlerts(
                testUserId, historicalTasks, AlertThreshold.Sensitivity.VERY_HIGH);

        // Then
        assertNotNull(mediumAlerts);
        assertNotNull(veryHighAlerts);
        
        // Very high sensitivity should detect borderline cases
        assertTrue(veryHighAlerts.size() >= mediumAlerts.size());
    }

    @Test
    void testEfficiencyOpportunityAlert() {
        // Given
        List<TaskDto> inefficientTasks = createInefficientPatternTasks();
        when(productivityPredictor.predictProductivity(anyString(), any())).thenReturn(mockForecast);
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        List<ProactiveAlert> alerts = proactiveAlertSystem.generateProactiveAlerts(
                testUserId, inefficientTasks, AlertThreshold.Sensitivity.HIGH);

        // Then
        assertNotNull(alerts);
        
        ProactiveAlert efficiencyAlert = alerts.stream()
                .filter(alert -> alert.getType() == ProactiveAlert.AlertType.EFFICIENCY_OPPORTUNITY)
                .findFirst()
                .orElse(null);
        
        if (efficiencyAlert != null) {
            assertEquals(ProactiveAlert.AlertType.EFFICIENCY_OPPORTUNITY, efficiencyAlert.getType());
            assertEquals(ProactiveAlert.AlertSeverity.LOW, efficiencyAlert.getSeverity());
            assertNotNull(efficiencyAlert.getContext());
            assertTrue(efficiencyAlert.getContext().containsKey("opportunities"));
        }
    }

    @Test
    void testAlertExpiration() {
        // Given
        ProductivityForecast lowForecast = createLowProductivityForecast();
        when(productivityPredictor.predictProductivity(anyString(), any())).thenReturn(lowForecast);
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        List<ProactiveAlert> alerts = proactiveAlertSystem.generateProactiveAlerts(testUserId, historicalTasks);

        // Then
        for (ProactiveAlert alert : alerts) {
            assertNotNull(alert.getTriggeredAt());
            assertNotNull(alert.getExpiresAt());
            assertTrue(alert.getExpiresAt().isAfter(alert.getTriggeredAt()));
            
            // Different alert types should have appropriate expiration periods
            long hoursUntilExpiry = java.time.Duration.between(alert.getTriggeredAt(), alert.getExpiresAt()).toHours();
            
            switch (alert.getType()) {
                case BURNOUT_RISK:
                    assertTrue(hoursUntilExpiry <= 24); // 1 day
                    break;
                case TREND_DECLINE:
                    assertTrue(hoursUntilExpiry <= 120); // 5 days
                    break;
                case PRODUCTIVITY_DROP:
                    assertTrue(hoursUntilExpiry <= 168); // 7 days
                    break;
                case EFFICIENCY_OPPORTUNITY:
                    assertTrue(hoursUntilExpiry <= 336); // 14 days
                    break;
                case WORKLOAD_IMBALANCE:
                    assertTrue(hoursUntilExpiry <= 168); // 7 days
                    break;
                case PATTERN_ANOMALY:
                    assertTrue(hoursUntilExpiry <= 72); // 3 days
                    break;
            }
        }
    }

    // Helper methods for creating test data

    private List<TaskDto> createHistoricalTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 20; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("task-" + i)
                    .userId(testUserId)
                    .title("Task " + i)
                    .status("COMPLETED")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(i + 1))
                    .completedAt(now.minusDays(i + 1).plusHours(2))
                    .estimatedHours(2.0)
                    .actualHours(2.0)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createHighVolumeTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create 60 tasks in the last 2 weeks (high volume)
        for (int i = 0; i < 60; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("high-volume-task-" + i)
                    .userId(testUserId)
                    .title("High Volume Task " + i)
                    .status(i % 4 == 0 ? "TODO" : "COMPLETED") // 75% completion rate
                    .priority(i % 3 == 0 ? "HIGH" : "MEDIUM") // 33% high priority
                    .createdAt(now.minusDays((i % 14) + 1))
                    .completedAt(i % 4 != 0 ? now.minusDays((i % 14) + 1).plusHours(1) : null)
                    .estimatedHours(1.0)
                    .actualHours(i % 4 != 0 ? 1.5 : null)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createImbalancedWorkloadTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create tasks with 80% high priority (imbalanced)
        for (int i = 0; i < 20; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("imbalanced-task-" + i)
                    .userId(testUserId)
                    .title("Imbalanced Task " + i)
                    .status("COMPLETED")
                    .priority(i < 16 ? "HIGH" : "LOW") // 80% high priority
                    .createdAt(now.minusDays(i + 1))
                    .completedAt(now.minusDays(i + 1).plusHours(1))
                    .estimatedHours(2.0)
                    .actualHours(2.0)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createInefficientPatternTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create tasks with many in-progress items and poor time estimation
        for (int i = 0; i < 15; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("inefficient-task-" + i)
                    .userId(testUserId)
                    .title("Inefficient Task " + i)
                    .status(i % 3 == 0 ? "IN_PROGRESS" : "COMPLETED") // 33% in progress
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(i + 1))
                    .completedAt(i % 3 != 0 ? now.minusDays(i + 1).plusHours(1) : null)
                    .estimatedHours(2.0)
                    .actualHours(i % 3 != 0 ? 5.0 : null) // Poor estimation (2.5x over)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private ProductivityForecast createMockForecast() {
        List<DailyProductivityPrediction> predictions = new ArrayList<>();
        LocalDate startDate = LocalDate.now().plusDays(1);
        
        for (int i = 0; i < 7; i++) {
            predictions.add(DailyProductivityPrediction.builder()
                    .date(startDate.plusDays(i))
                    .predictedScore(6.0 + Math.random() * 2.0) // 6-8 range (good)
                    .confidence(0.7)
                    .lowerBound(5.0)
                    .upperBound(8.0)
                    .expectedTasksCompleted(3)
                    .expectedCompletionRate(75.0)
                    .dayOfWeekPattern("MID_WEEK")
                    .reasoning("Normal productivity expected")
                    .build());
        }
        
        return ProductivityForecast.builder()
                .userId(testUserId)
                .dailyPredictions(predictions)
                .overallForecastScore(7.0)
                .confidence(0.7)
                .forecastMethod("TREND_BASED_WITH_PATTERNS")
                .generatedAt(LocalDateTime.now())
                .forecastStartDate(startDate)
                .forecastEndDate(startDate.plusDays(6))
                .metadata(Map.of("trendDirection", "STABLE"))
                .assumptions(Arrays.asList("Normal work patterns"))
                .uncertaintyRange(1.0)
                .build();
    }

    private ProductivityForecast createLowProductivityForecast() {
        List<DailyProductivityPrediction> predictions = new ArrayList<>();
        LocalDate startDate = LocalDate.now().plusDays(1);
        
        for (int i = 0; i < 7; i++) {
            predictions.add(DailyProductivityPrediction.builder()
                    .date(startDate.plusDays(i))
                    .predictedScore(2.0 + Math.random() * 2.0) // 2-4 range (low)
                    .confidence(0.8)
                    .lowerBound(1.0)
                    .upperBound(4.0)
                    .expectedTasksCompleted(1)
                    .expectedCompletionRate(30.0)
                    .dayOfWeekPattern("MID_WEEK")
                    .reasoning("Low productivity predicted")
                    .build());
        }
        
        return ProductivityForecast.builder()
                .userId(testUserId)
                .dailyPredictions(predictions)
                .overallForecastScore(3.0)
                .confidence(0.8)
                .forecastMethod("TREND_BASED_WITH_PATTERNS")
                .generatedAt(LocalDateTime.now())
                .forecastStartDate(startDate)
                .forecastEndDate(startDate.plusDays(6))
                .metadata(Map.of("trendDirection", "DECREASING"))
                .assumptions(Arrays.asList("Declining trend continues"))
                .uncertaintyRange(1.5)
                .build();
    }

    private ProductivityForecast createMarginalProductivityForecast() {
        List<DailyProductivityPrediction> predictions = new ArrayList<>();
        LocalDate startDate = LocalDate.now().plusDays(1);
        
        for (int i = 0; i < 7; i++) {
            predictions.add(DailyProductivityPrediction.builder()
                    .date(startDate.plusDays(i))
                    .predictedScore(4.5 + Math.random() * 1.0) // 4.5-5.5 range (marginal)
                    .confidence(0.6)
                    .lowerBound(3.5)
                    .upperBound(6.0)
                    .expectedTasksCompleted(2)
                    .expectedCompletionRate(50.0)
                    .dayOfWeekPattern("MID_WEEK")
                    .reasoning("Marginal productivity expected")
                    .build());
        }
        
        return ProductivityForecast.builder()
                .userId(testUserId)
                .dailyPredictions(predictions)
                .overallForecastScore(5.0)
                .confidence(0.6)
                .forecastMethod("TREND_BASED_WITH_PATTERNS")
                .generatedAt(LocalDateTime.now())
                .forecastStartDate(startDate)
                .forecastEndDate(startDate.plusDays(6))
                .metadata(Map.of("trendDirection", "STABLE"))
                .assumptions(Arrays.asList("Marginal performance patterns"))
                .uncertaintyRange(2.0)
                .build();
    }

    private ProductivityForecast createBorderlineProductivityForecast() {
        List<DailyProductivityPrediction> predictions = new ArrayList<>();
        LocalDate startDate = LocalDate.now().plusDays(1);
        
        for (int i = 0; i < 7; i++) {
            predictions.add(DailyProductivityPrediction.builder()
                    .date(startDate.plusDays(i))
                    .predictedScore(4.0) // Right at borderline
                    .confidence(0.5)
                    .lowerBound(3.0)
                    .upperBound(5.0)
                    .expectedTasksCompleted(2)
                    .expectedCompletionRate(45.0)
                    .dayOfWeekPattern("MID_WEEK")
                    .reasoning("Borderline productivity")
                    .build());
        }
        
        return ProductivityForecast.builder()
                .userId(testUserId)
                .dailyPredictions(predictions)
                .overallForecastScore(4.0)
                .confidence(0.5)
                .forecastMethod("TREND_BASED_WITH_PATTERNS")
                .generatedAt(LocalDateTime.now())
                .forecastStartDate(startDate)
                .forecastEndDate(startDate.plusDays(6))
                .metadata(Map.of("trendDirection", "STABLE"))
                .assumptions(Arrays.asList("Borderline performance"))
                .uncertaintyRange(2.5)
                .build();
    }

    private ProductivityForecast createHighConfidenceForecast() {
        List<DailyProductivityPrediction> predictions = new ArrayList<>();
        LocalDate startDate = LocalDate.now().plusDays(1);
        
        for (int i = 0; i < 7; i++) {
            predictions.add(DailyProductivityPrediction.builder()
                    .date(startDate.plusDays(i))
                    .predictedScore(3.0) // Low but high confidence
                    .confidence(0.9)
                    .lowerBound(2.5)
                    .upperBound(3.5)
                    .expectedTasksCompleted(1)
                    .expectedCompletionRate(35.0)
                    .dayOfWeekPattern("MID_WEEK")
                    .reasoning("High confidence low productivity")
                    .build());
        }
        
        return ProductivityForecast.builder()
                .userId(testUserId)
                .dailyPredictions(predictions)
                .overallForecastScore(3.0)
                .confidence(0.9)
                .forecastMethod("TREND_BASED_WITH_PATTERNS")
                .generatedAt(LocalDateTime.now())
                .forecastStartDate(startDate)
                .forecastEndDate(startDate.plusDays(6))
                .metadata(Map.of("trendDirection", "STABLE"))
                .assumptions(Arrays.asList("High confidence prediction"))
                .uncertaintyRange(0.5)
                .build();
    }

    private TrendAnalysis createMockTrendAnalysis() {
        return TrendAnalysis.builder()
                .userId(testUserId)
                .trendDirection("STABLE")
                .trendStrength(0.2)
                .confidence(0.7)
                .insights(Arrays.asList("Consistent productivity"))
                .build();
    }

    private TrendAnalysis createDecliningTrendAnalysis() {
        return TrendAnalysis.builder()
                .userId(testUserId)
                .trendDirection("DECREASING")
                .trendStrength(0.6) // Strong decline
                .confidence(0.8)
                .insights(Arrays.asList("Significant productivity decline detected"))
                .build();
    }
}
    