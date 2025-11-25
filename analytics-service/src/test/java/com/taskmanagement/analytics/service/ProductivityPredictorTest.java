package com.taskmanagement.analytics.service;

import com.taskmanagement.analytics.dto.TaskDto;
import com.taskmanagement.analytics.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductivityPredictorTest {

    @Mock
    private AnalyticsCalculator analyticsCalculator;

    @Mock
    private TrendAnalysisEngine trendAnalysisEngine;

    @InjectMocks
    private ProductivityPredictor productivityPredictor;

    private String testUserId;
    private List<TaskDto> historicalTasks;
    private TrendAnalysis mockTrendAnalysis;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
        historicalTasks = createHistoricalTasks();
        mockTrendAnalysis = createMockTrendAnalysis();
    }

    @Test
    void testPredictProductivity_WithSufficientData() {
        // Given
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, historicalTasks, 7);

        // Then
        assertNotNull(forecast);
        assertEquals(testUserId, forecast.getUserId());
        assertEquals(7, forecast.getDailyPredictions().size());
        assertTrue(forecast.getConfidence() > 0.0);
        assertTrue(forecast.getOverallForecastScore() >= 0.0);
        assertTrue(forecast.getOverallForecastScore() <= 10.0);
        assertEquals("TREND_BASED_WITH_PATTERNS", forecast.getForecastMethod());
        assertNotNull(forecast.getAssumptions());
        assertTrue(forecast.getAssumptions().size() > 0);
    }

    @Test
    void testPredictProductivity_WithEmptyData() {
        // Given
        List<TaskDto> emptyTasks = new ArrayList<>();

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, emptyTasks, 7);

        // Then
        assertNotNull(forecast);
        assertEquals(testUserId, forecast.getUserId());
        assertEquals(7, forecast.getDailyPredictions().size());
        assertEquals(0.3, forecast.getConfidence(), 0.01); // Low confidence
        assertEquals(5.0, forecast.getOverallForecastScore(), 0.01); // Neutral score
        assertEquals("DEFAULT_BASELINE", forecast.getForecastMethod());
        
        // Check that all predictions have neutral values
        for (DailyProductivityPrediction prediction : forecast.getDailyPredictions()) {
            assertEquals(5.0, prediction.getPredictedScore(), 0.01);
            assertEquals(0.3, prediction.getConfidence(), 0.01);
            assertEquals("Insufficient historical data for accurate prediction", prediction.getReasoning());
        }
    }

    @Test
    void testPredictProductivity_DefaultSevenDays() {
        // Given
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, historicalTasks);

        // Then
        assertNotNull(forecast);
        assertEquals(7, forecast.getDailyPredictions().size());
        assertEquals(LocalDate.now().plusDays(1), forecast.getForecastStartDate());
        assertEquals(LocalDate.now().plusDays(7), forecast.getForecastEndDate());
    }

    @Test
    void testDailyPredictions_ConfidenceDecreases() {
        // Given
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, historicalTasks, 7);

        // Then
        List<DailyProductivityPrediction> predictions = forecast.getDailyPredictions();
        
        // Confidence should generally decrease with distance into future
        for (int i = 1; i < predictions.size(); i++) {
            assertTrue(predictions.get(i).getConfidence() <= predictions.get(i-1).getConfidence() + 0.1, 
                    "Confidence should not increase significantly with time");
        }
    }

    @Test
    void testDailyPredictions_ValidBounds() {
        // Given
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, historicalTasks, 5);

        // Then
        for (DailyProductivityPrediction prediction : forecast.getDailyPredictions()) {
            // Predicted score should be within valid range
            assertTrue(prediction.getPredictedScore() >= 0.0);
            assertTrue(prediction.getPredictedScore() <= 10.0);
            
            // Bounds should be logical
            assertTrue(prediction.getLowerBound() <= prediction.getPredictedScore());
            assertTrue(prediction.getUpperBound() >= prediction.getPredictedScore());
            assertTrue(prediction.getLowerBound() >= 0.0);
            assertTrue(prediction.getUpperBound() <= 10.0);
            
            // Expected tasks should be non-negative
            assertTrue(prediction.getExpectedTasksCompleted() >= 0);
            
            // Completion rate should be valid percentage
            assertTrue(prediction.getExpectedCompletionRate() >= 0.0);
            assertTrue(prediction.getExpectedCompletionRate() <= 100.0);
            
            // Should have day pattern and reasoning
            assertNotNull(prediction.getDayOfWeekPattern());
            assertNotNull(prediction.getReasoning());
        }
    }

    @Test
    void testDayOfWeekPatterns() {
        // Given
        List<TaskDto> weekdayTasks = createWeekdayFocusedTasks();
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, weekdayTasks, 7);

        // Then
        List<DailyProductivityPrediction> predictions = forecast.getDailyPredictions();
        
        // Check that day patterns are correctly assigned
        for (DailyProductivityPrediction prediction : predictions) {
            DayOfWeek dayOfWeek = prediction.getDate().getDayOfWeek();
            String expectedPattern = getDayOfWeekPattern(dayOfWeek);
            assertEquals(expectedPattern, prediction.getDayOfWeekPattern());
        }
    }

    @Test
    void testTrendAdjustment_IncreasingTrend() {
        // Given
        TrendAnalysis increasingTrend = TrendAnalysis.builder()
                .userId(testUserId)
                .trendDirection("INCREASING")
                .trendStrength(0.5)
                .confidence(0.8)
                .build();
        
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(increasingTrend);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, historicalTasks, 3);

        // Then
        // With increasing trend, earlier predictions should generally be higher
        List<DailyProductivityPrediction> predictions = forecast.getDailyPredictions();
        assertNotNull(predictions);
        assertTrue(predictions.size() > 0);
        
        // Overall forecast should reflect positive trend
        assertTrue(forecast.getOverallForecastScore() >= 5.0);
    }

    @Test
    void testTrendAdjustment_DecreasingTrend() {
        // Given
        TrendAnalysis decreasingTrend = TrendAnalysis.builder()
                .userId(testUserId)
                .trendDirection("DECREASING")
                .trendStrength(0.4)
                .confidence(0.7)
                .build();
        
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(decreasingTrend);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, historicalTasks, 3);

        // Then
        assertNotNull(forecast);
        // With decreasing trend, predictions might be lower
        assertTrue(forecast.getOverallForecastScore() >= 0.0);
    }

    @Test
    void testForecastMetadata() {
        // Given
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, historicalTasks, 5);

        // Then
        assertNotNull(forecast.getMetadata());
        assertTrue(forecast.getMetadata().containsKey("trendDirection"));
        assertTrue(forecast.getMetadata().containsKey("trendStrength"));
        assertTrue(forecast.getMetadata().containsKey("forecastAlgorithm"));
        assertEquals("PATTERN_BASED_WITH_TREND_ADJUSTMENT", forecast.getMetadata().get("forecastAlgorithm"));
    }

    @Test
    void testUncertaintyRange() {
        // Given
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, historicalTasks, 7);

        // Then
        assertTrue(forecast.getUncertaintyRange() >= 0.0);
        assertTrue(forecast.getUncertaintyRange() <= 3.0); // Max uncertainty
        
        // Higher confidence should mean lower uncertainty
        if (forecast.getConfidence() > 0.8) {
            assertTrue(forecast.getUncertaintyRange() < 1.0);
        }
    }

    @Test
    void testConsistentTaskVelocity() {
        // Given
        List<TaskDto> consistentTasks = createConsistentVelocityTasks();
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, consistentTasks, 7);

        // Then
        // With consistent velocity, expected task counts should be reasonable
        for (DailyProductivityPrediction prediction : forecast.getDailyPredictions()) {
            assertTrue(prediction.getExpectedTasksCompleted() >= 0);
            assertTrue(prediction.getExpectedTasksCompleted() <= 20); // Reasonable upper bound
        }
    }

    @Test
    void testHighPriorityTaskImpact() {
        // Given
        List<TaskDto> highPriorityTasks = createHighPriorityTasks();
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, highPriorityTasks, 5);

        // Then
        // High priority tasks should generally lead to higher productivity scores
        assertTrue(forecast.getOverallForecastScore() >= 5.0);
        assertTrue(forecast.getConfidence() > 0.0);
    }

    @Test
    void testPredictionAccuracy_WithKnownHistoricalPatterns() {
        // Given - Create tasks with known Monday productivity drop pattern
        List<TaskDto> mondayDropTasks = createMondayProductivityDropTasks();
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, mondayDropTasks, 7);

        // Then
        List<DailyProductivityPrediction> predictions = forecast.getDailyPredictions();
        
        // Find Monday predictions and verify they're lower than other weekdays
        DailyProductivityPrediction mondayPrediction = predictions.stream()
                .filter(p -> p.getDate().getDayOfWeek() == DayOfWeek.MONDAY)
                .findFirst()
                .orElse(null);
        
        if (mondayPrediction != null) {
            double avgWeekdayScore = predictions.stream()
                    .filter(p -> p.getDate().getDayOfWeek() != DayOfWeek.MONDAY)
                    .filter(p -> p.getDate().getDayOfWeek().getValue() <= 5)
                    .mapToDouble(DailyProductivityPrediction::getPredictedScore)
                    .average()
                    .orElse(5.0);
            
            // Monday should be predicted lower than average weekday
            assertTrue(mondayPrediction.getPredictedScore() <= avgWeekdayScore + 0.5,
                    "Monday prediction should reflect historical pattern");
            assertEquals("WEEK_START", mondayPrediction.getDayOfWeekPattern());
        }
    }

    @Test
    void testConfidenceScoring_WithVariousDataQuality() {
        // Test 1: High quality data (lots of recent tasks)
        List<TaskDto> highQualityTasks = createHighQualityDataTasks();
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);
        
        ProductivityForecast highQualityForecast = productivityPredictor.predictProductivity(testUserId, highQualityTasks, 7);
        
        // Test 2: Low quality data (few, old tasks)
        List<TaskDto> lowQualityTasks = createLowQualityDataTasks();
        ProductivityForecast lowQualityForecast = productivityPredictor.predictProductivity(testUserId, lowQualityTasks, 7);
        
        // Test 3: Medium quality data
        List<TaskDto> mediumQualityTasks = createMediumQualityDataTasks();
        ProductivityForecast mediumQualityForecast = productivityPredictor.predictProductivity(testUserId, mediumQualityTasks, 7);
        
        // Then - Confidence should correlate with data quality
        assertTrue(highQualityForecast.getConfidence() > mediumQualityForecast.getConfidence(),
                "High quality data should have higher confidence");
        assertTrue(mediumQualityForecast.getConfidence() > lowQualityForecast.getConfidence(),
                "Medium quality data should have higher confidence than low quality");
        
        // All confidences should be in valid range
        assertTrue(highQualityForecast.getConfidence() >= 0.3 && highQualityForecast.getConfidence() <= 1.0);
        assertTrue(mediumQualityForecast.getConfidence() >= 0.3 && mediumQualityForecast.getConfidence() <= 1.0);
        assertTrue(lowQualityForecast.getConfidence() >= 0.3 && lowQualityForecast.getConfidence() <= 1.0);
    }

    @Test
    void testPredictionAccuracy_WeekendVsWeekdayPatterns() {
        // Given
        List<TaskDto> weekendWorkTasks = createWeekendWorkTasks();
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, weekendWorkTasks, 14);

        // Then
        List<DailyProductivityPrediction> predictions = forecast.getDailyPredictions();
        
        double avgWeekendScore = predictions.stream()
                .filter(p -> p.getDate().getDayOfWeek() == DayOfWeek.SATURDAY || 
                           p.getDate().getDayOfWeek() == DayOfWeek.SUNDAY)
                .mapToDouble(DailyProductivityPrediction::getPredictedScore)
                .average()
                .orElse(5.0);
        
        double avgWeekdayScore = predictions.stream()
                .filter(p -> p.getDate().getDayOfWeek().getValue() <= 5)
                .mapToDouble(DailyProductivityPrediction::getPredictedScore)
                .average()
                .orElse(5.0);
        
        // Weekend predictions should reflect historical weekend work patterns
        assertNotEquals(avgWeekendScore, avgWeekdayScore, 0.1);
        
        // Verify weekend patterns are correctly identified
        predictions.stream()
                .filter(p -> p.getDate().getDayOfWeek() == DayOfWeek.SATURDAY || 
                           p.getDate().getDayOfWeek() == DayOfWeek.SUNDAY)
                .forEach(p -> assertEquals("WEEKEND", p.getDayOfWeekPattern()));
    }

    @Test
    void testConfidenceScoring_WithTrendAnalysisQuality() {
        // Given - High confidence trend analysis
        TrendAnalysis highConfidenceTrend = TrendAnalysis.builder()
                .userId(testUserId)
                .trendDirection("INCREASING")
                .trendStrength(0.4)
                .confidence(0.9) // High confidence
                .insights(Arrays.asList("Strong upward trend"))
                .build();
        
        // Low confidence trend analysis
        TrendAnalysis lowConfidenceTrend = TrendAnalysis.builder()
                .userId(testUserId)
                .trendDirection("STABLE")
                .trendStrength(0.1)
                .confidence(0.3) // Low confidence
                .insights(Arrays.asList("Unclear trend"))
                .build();
        
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class)))
                .thenReturn(highConfidenceTrend)
                .thenReturn(lowConfidenceTrend);

        // When
        ProductivityForecast highTrendConfidenceForecast = productivityPredictor.predictProductivity(testUserId, historicalTasks, 7);
        ProductivityForecast lowTrendConfidenceForecast = productivityPredictor.predictProductivity(testUserId, historicalTasks, 7);

        // Then
        assertTrue(highTrendConfidenceForecast.getConfidence() > lowTrendConfidenceForecast.getConfidence(),
                "Forecast confidence should reflect trend analysis confidence");
    }

    @Test
    void testPredictionAccuracy_ConsistencyPatterns() {
        // Given - Highly consistent task completion pattern
        List<TaskDto> consistentTasks = createHighlyConsistentTasks();
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast consistentForecast = productivityPredictor.predictProductivity(testUserId, consistentTasks, 7);

        // Then
        // Consistent patterns should have higher confidence
        assertTrue(consistentForecast.getConfidence() > 0.6);
        
        // Uncertainty range should be smaller for consistent patterns
        assertTrue(consistentForecast.getUncertaintyRange() < 2.0);
        
        // Daily predictions should have smaller confidence intervals
        for (DailyProductivityPrediction prediction : consistentForecast.getDailyPredictions()) {
            double intervalSize = prediction.getUpperBound() - prediction.getLowerBound();
            assertTrue(intervalSize < 3.0, "Consistent patterns should have tighter prediction intervals");
        }
    }

    @Test
    void testPredictionAccuracy_VolatilePatterns() {
        // Given - Highly volatile task completion pattern
        List<TaskDto> volatileTasks = createVolatilePatternTasks();
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast volatileForecast = productivityPredictor.predictProductivity(testUserId, volatileTasks, 7);

        // Then
        // Volatile patterns should have lower confidence
        assertTrue(volatileForecast.getConfidence() < 0.8);
        
        // Uncertainty range should be larger for volatile patterns
        assertTrue(volatileForecast.getUncertaintyRange() > 1.0);
        
        // Daily predictions should have wider confidence intervals
        for (DailyProductivityPrediction prediction : volatileForecast.getDailyPredictions()) {
            double intervalSize = prediction.getUpperBound() - prediction.getLowerBound();
            assertTrue(intervalSize > 1.0, "Volatile patterns should have wider prediction intervals");
        }
    }

    @Test
    void testPredictionAccuracy_RecentDataRecency() {
        // Given - Tasks with recent activity
        List<TaskDto> recentTasks = createRecentActivityTasks();
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);
        
        // Tasks with old activity
        List<TaskDto> oldTasks = createOldActivityTasks();

        // When
        ProductivityForecast recentForecast = productivityPredictor.predictProductivity(testUserId, recentTasks, 7);
        ProductivityForecast oldForecast = productivityPredictor.predictProductivity(testUserId, oldTasks, 7);

        // Then
        // Recent data should have higher confidence
        assertTrue(recentForecast.getConfidence() > oldForecast.getConfidence(),
                "Recent data should produce higher confidence predictions");
    }

    @Test
    void testAlertThresholdBehavior_PredictionAccuracy() {
        // Given - Tasks that should trigger productivity drop alerts
        List<TaskDto> decliningTasks = createDecliningProductivityTasks();
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, decliningTasks, 7);

        // Then
        // Should predict low productivity scores that would trigger alerts
        assertTrue(forecast.getOverallForecastScore() < 5.0);
        
        // Should have predictions below alert thresholds (4.0)
        long lowPredictionDays = forecast.getDailyPredictions().stream()
                .filter(p -> p.getPredictedScore() < 4.0)
                .count();
        
        assertTrue(lowPredictionDays > 0, "Should predict some days below alert threshold");
        
        // Confidence should be reasonable for triggering alerts
        assertTrue(forecast.getConfidence() > 0.5, "Should have sufficient confidence for alert generation");
    }

    @Test
    void testPredictionBounds_ValidityAndLogic() {
        // Given
        when(trendAnalysisEngine.analyzeTrends(anyString(), any(List.class))).thenReturn(mockTrendAnalysis);

        // When
        ProductivityForecast forecast = productivityPredictor.predictProductivity(testUserId, historicalTasks, 10);

        // Then
        for (DailyProductivityPrediction prediction : forecast.getDailyPredictions()) {
            // Bounds should be logical
            assertTrue(prediction.getLowerBound() <= prediction.getPredictedScore());
            assertTrue(prediction.getUpperBound() >= prediction.getPredictedScore());
            
            // Bounds should be within valid productivity score range
            assertTrue(prediction.getLowerBound() >= 0.0);
            assertTrue(prediction.getUpperBound() <= 10.0);
            
            // Confidence intervals should make sense
            double intervalSize = prediction.getUpperBound() - prediction.getLowerBound();
            assertTrue(intervalSize > 0.0, "Confidence interval should have positive size");
            assertTrue(intervalSize <= 10.0, "Confidence interval should not exceed full scale");
            
            // Lower confidence should generally mean wider intervals
            if (prediction.getConfidence() < 0.5) {
                assertTrue(intervalSize > 1.0, "Low confidence should have wider intervals");
            }
        }
    }

    // Helper methods for creating test data

    private List<TaskDto> createHistoricalTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create 30 days of historical tasks with varying patterns
        for (int i = 0; i < 30; i++) {
            LocalDateTime createdDate = now.minusDays(i + 1);
            
            // More tasks on weekdays, fewer on weekends
            int tasksPerDay = createdDate.getDayOfWeek().getValue() <= 5 ? 3 : 1;
            
            for (int j = 0; j < tasksPerDay; j++) {
                TaskDto task = TaskDto.builder()
                        .taskId("task-" + i + "-" + j)
                        .userId(testUserId)
                        .title("Historical Task " + i + "-" + j)
                        .status(Math.random() > 0.2 ? "COMPLETED" : "TODO") // 80% completion rate
                        .priority(Math.random() > 0.7 ? "HIGH" : "MEDIUM")
                        .createdAt(createdDate)
                        .completedAt(Math.random() > 0.2 ? createdDate.plusHours((long)(Math.random() * 8 + 1)) : null)
                        .estimatedHours(2.0 + Math.random() * 3.0)
                        .actualHours(Math.random() > 0.2 ? 2.0 + Math.random() * 3.0 : null)
                        .build();
                tasks.add(task);
            }
        }
        
        return tasks;
    }

    private List<TaskDto> createWeekdayFocusedTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create tasks only on weekdays for the past 14 days
        for (int i = 0; i < 14; i++) {
            LocalDateTime date = now.minusDays(i + 1);
            if (date.getDayOfWeek().getValue() <= 5) { // Weekdays only
                TaskDto task = TaskDto.builder()
                        .taskId("weekday-task-" + i)
                        .userId(testUserId)
                        .title("Weekday Task " + i)
                        .status("COMPLETED")
                        .priority("MEDIUM")
                        .createdAt(date)
                        .completedAt(date.plusHours(2))
                        .estimatedHours(2.0)
                        .actualHours(2.0)
                        .build();
                tasks.add(task);
            }
        }
        
        return tasks;
    }

    private List<TaskDto> createConsistentVelocityTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create exactly 2 tasks per day for 20 days
        for (int i = 0; i < 20; i++) {
            LocalDateTime date = now.minusDays(i + 1);
            
            for (int j = 0; j < 2; j++) {
                TaskDto task = TaskDto.builder()
                        .taskId("consistent-task-" + i + "-" + j)
                        .userId(testUserId)
                        .title("Consistent Task " + i + "-" + j)
                        .status("COMPLETED")
                        .priority("MEDIUM")
                        .createdAt(date)
                        .completedAt(date.plusHours(j + 1))
                        .estimatedHours(1.0)
                        .actualHours(1.0)
                        .build();
                tasks.add(task);
            }
        }
        
        return tasks;
    }

    private List<TaskDto> createHighPriorityTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 15; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("high-priority-task-" + i)
                    .userId(testUserId)
                    .title("High Priority Task " + i)
                    .status("COMPLETED")
                    .priority("HIGH")
                    .createdAt(now.minusDays(i + 1))
                    .completedAt(now.minusDays(i + 1).plusHours(1))
                    .estimatedHours(3.0)
                    .actualHours(3.0)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private TrendAnalysis createMockTrendAnalysis() {
        return TrendAnalysis.builder()
                .userId(testUserId)
                .trendDirection("STABLE")
                .trendStrength(0.2)
                .confidence(0.7)
                .insights(Arrays.asList("Consistent productivity pattern", "Good work-life balance"))
                .build();
    }

    private List<TaskDto> createMondayProductivityDropTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create 4 weeks of data with Monday productivity drops
        for (int week = 0; week < 4; week++) {
            for (int day = 0; day < 7; day++) {
                LocalDateTime date = now.minusWeeks(week).minusDays(day);
                DayOfWeek dayOfWeek = date.getDayOfWeek();
                
                // Monday gets fewer completed tasks (productivity drop pattern)
                int tasksPerDay = dayOfWeek == DayOfWeek.MONDAY ? 1 : 3;
                
                for (int i = 0; i < tasksPerDay; i++) {
                    TaskDto task = TaskDto.builder()
                            .taskId("monday-drop-" + week + "-" + day + "-" + i)
                            .userId(testUserId)
                            .title("Monday Drop Task")
                            .status("COMPLETED")
                            .priority("MEDIUM")
                            .createdAt(date)
                            .completedAt(date.plusHours(2))
                            .estimatedHours(2.0)
                            .actualHours(2.0)
                            .build();
                    tasks.add(task);
                }
            }
        }
        
        return tasks;
    }

    private List<TaskDto> createHighQualityDataTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // 50 recent tasks with consistent patterns
        for (int i = 0; i < 50; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("high-quality-" + i)
                    .userId(testUserId)
                    .title("High Quality Task " + i)
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

    private List<TaskDto> createLowQualityDataTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Only 5 old tasks
        for (int i = 0; i < 5; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("low-quality-" + i)
                    .userId(testUserId)
                    .title("Low Quality Task " + i)
                    .status("COMPLETED")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(25 + i)) // Old data
                    .completedAt(now.minusDays(25 + i).plusHours(2))
                    .estimatedHours(2.0)
                    .actualHours(2.0)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createMediumQualityDataTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // 15 moderately recent tasks
        for (int i = 0; i < 15; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("medium-quality-" + i)
                    .userId(testUserId)
                    .title("Medium Quality Task " + i)
                    .status("COMPLETED")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(i + 5)) // Somewhat recent
                    .completedAt(now.minusDays(i + 5).plusHours(2))
                    .estimatedHours(2.0)
                    .actualHours(2.0)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createWeekendWorkTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create tasks with weekend work pattern
        for (int week = 0; week < 6; week++) {
            for (int day = 0; day < 7; day++) {
                LocalDateTime date = now.minusWeeks(week).minusDays(day);
                DayOfWeek dayOfWeek = date.getDayOfWeek();
                
                // Weekend work pattern: some work on weekends, more on weekdays
                int tasksPerDay;
                if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                    tasksPerDay = 1; // Light weekend work
                } else {
                    tasksPerDay = 4; // Heavy weekday work
                }
                
                for (int i = 0; i < tasksPerDay; i++) {
                    TaskDto task = TaskDto.builder()
                            .taskId("weekend-work-" + week + "-" + day + "-" + i)
                            .userId(testUserId)
                            .title("Weekend Work Task")
                            .status("COMPLETED")
                            .priority("MEDIUM")
                            .createdAt(date)
                            .completedAt(date.plusHours(1))
                            .estimatedHours(1.0)
                            .actualHours(1.0)
                            .build();
                    tasks.add(task);
                }
            }
        }
        
        return tasks;
    }

    private List<TaskDto> createHighlyConsistentTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Exactly 3 tasks per day for 20 days (highly consistent)
        for (int i = 0; i < 20; i++) {
            LocalDateTime date = now.minusDays(i + 1);
            
            for (int j = 0; j < 3; j++) {
                TaskDto task = TaskDto.builder()
                        .taskId("consistent-" + i + "-" + j)
                        .userId(testUserId)
                        .title("Consistent Task " + i + "-" + j)
                        .status("COMPLETED")
                        .priority("MEDIUM")
                        .createdAt(date)
                        .completedAt(date.plusHours(j + 1))
                        .estimatedHours(1.0)
                        .actualHours(1.0)
                        .build();
                tasks.add(task);
            }
        }
        
        return tasks;
    }

    private List<TaskDto> createVolatilePatternTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Highly variable task completion (0-8 tasks per day randomly)
        for (int i = 0; i < 20; i++) {
            LocalDateTime date = now.minusDays(i + 1);
            int tasksPerDay = (int) (Math.random() * 9); // 0-8 tasks randomly
            
            for (int j = 0; j < tasksPerDay; j++) {
                TaskDto task = TaskDto.builder()
                        .taskId("volatile-" + i + "-" + j)
                        .userId(testUserId)
                        .title("Volatile Task " + i + "-" + j)
                        .status("COMPLETED")
                        .priority("MEDIUM")
                        .createdAt(date)
                        .completedAt(date.plusHours(j + 1))
                        .estimatedHours(1.0)
                        .actualHours(1.0)
                        .build();
                tasks.add(task);
            }
        }
        
        return tasks;
    }

    private List<TaskDto> createRecentActivityTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Tasks completed in the last 3 days
        for (int i = 0; i < 15; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("recent-" + i)
                    .userId(testUserId)
                    .title("Recent Task " + i)
                    .status("COMPLETED")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(i % 3 + 1)) // Last 3 days
                    .completedAt(now.minusDays(i % 3 + 1).plusHours(2))
                    .estimatedHours(2.0)
                    .actualHours(2.0)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createOldActivityTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Tasks completed 20-30 days ago
        for (int i = 0; i < 15; i++) {
            TaskDto task = TaskDto.builder()
                    .taskId("old-" + i)
                    .userId(testUserId)
                    .title("Old Task " + i)
                    .status("COMPLETED")
                    .priority("MEDIUM")
                    .createdAt(now.minusDays(20 + i)) // 20-35 days ago
                    .completedAt(now.minusDays(20 + i).plusHours(2))
                    .estimatedHours(2.0)
                    .actualHours(2.0)
                    .build();
            tasks.add(task);
        }
        
        return tasks;
    }

    private List<TaskDto> createDecliningProductivityTasks() {
        List<TaskDto> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create declining pattern: more tasks completed in earlier days
        for (int i = 0; i < 14; i++) {
            LocalDateTime date = now.minusDays(i + 1);
            
            // Declining pattern: more tasks in the past, fewer recently
            int tasksPerDay = Math.max(1, 5 - (i / 3)); // 5, 4, 3, 2, 1 pattern
            
            for (int j = 0; j < tasksPerDay; j++) {
                TaskDto task = TaskDto.builder()
                        .taskId("declining-" + i + "-" + j)
                        .userId(testUserId)
                        .title("Declining Task " + i + "-" + j)
                        .status("COMPLETED")
                        .priority("MEDIUM")
                        .createdAt(date)
                        .completedAt(date.plusHours(j + 1))
                        .estimatedHours(1.0)
                        .actualHours(1.0)
                        .build();
                tasks.add(task);
            }
        }
        
        return tasks;
    }

    private String getDayOfWeekPattern(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "WEEK_START";
            case TUESDAY:
            case WEDNESDAY:
            case THURSDAY: return "MID_WEEK";
            case FRIDAY: return "WEEK_END";
            case SATURDAY:
            case SUNDAY: return "WEEKEND";
            default: return "UNKNOWN";
        }
    }
}
