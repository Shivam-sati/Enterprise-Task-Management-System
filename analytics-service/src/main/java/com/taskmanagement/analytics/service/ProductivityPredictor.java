package com.taskmanagement.analytics.service;

import com.taskmanagement.analytics.dto.TaskDto;
import com.taskmanagement.analytics.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductivityPredictor {

    private final TrendAnalysisEngine trendAnalysisEngine;

    private static final int DEFAULT_FORECAST_DAYS = 7;
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.6;

    /**
     * Generate a 7-day productivity forecast based on historical patterns
     */
    public ProductivityForecast predictProductivity(String userId, List<TaskDto> historicalTasks, int forecastDays) {
        log.info("Generating {}-day productivity forecast for user: {}", forecastDays, userId);
        
        if (historicalTasks.isEmpty()) {
            return createEmptyForecast(userId, forecastDays);
        }

        // Analyze historical patterns
        Map<String, Object> historicalAnalysis = analyzeHistoricalPatterns(historicalTasks);
        
        // Generate trend analysis
        TrendAnalysis trendAnalysis = trendAnalysisEngine.analyzeTrends(userId, historicalTasks);
        
        // Create daily predictions
        List<DailyProductivityPrediction> dailyPredictions = generateDailyPredictions(
            historicalAnalysis, trendAnalysis, forecastDays);
        
        // Calculate overall forecast metrics
        double overallScore = calculateOverallForecastScore(dailyPredictions);
        double confidence = calculateForecastConfidence(historicalTasks, trendAnalysis);
        
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(forecastDays - 1);
        
        return ProductivityForecast.builder()
                .userId(userId)
                .dailyPredictions(dailyPredictions)
                .overallForecastScore(overallScore)
                .confidence(confidence)
                .forecastMethod("TREND_BASED_WITH_PATTERNS")
                .generatedAt(LocalDateTime.now())
                .forecastStartDate(startDate)
                .forecastEndDate(endDate)
                .metadata(createForecastMetadata(historicalAnalysis, trendAnalysis))
                .assumptions(createForecastAssumptions())
                .uncertaintyRange(calculateUncertaintyRange(confidence))
                .build();
    }

    /**
     * Generate productivity forecast with default 7-day period
     */
    public ProductivityForecast predictProductivity(String userId, List<TaskDto> historicalTasks) {
        return predictProductivity(userId, historicalTasks, DEFAULT_FORECAST_DAYS);
    }

    private Map<String, Object> analyzeHistoricalPatterns(List<TaskDto> tasks) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Day of week patterns
        Map<DayOfWeek, Double> dayOfWeekProductivity = analyzeDayOfWeekPatterns(tasks);
        analysis.put("dayOfWeekPatterns", dayOfWeekProductivity);
        
        // Weekly productivity trends
        Map<String, Double> weeklyTrends = analyzeWeeklyTrends(tasks);
        analysis.put("weeklyTrends", weeklyTrends);
        
        // Task completion velocity
        double averageVelocity = calculateAverageVelocity(tasks);
        analysis.put("averageVelocity", averageVelocity);
        
        // Productivity consistency
        double consistencyScore = calculateConsistencyScore(tasks);
        analysis.put("consistencyScore", consistencyScore);
        
        return analysis;
    }

    private Map<DayOfWeek, Double> analyzeDayOfWeekPatterns(List<TaskDto> tasks) {
        Map<DayOfWeek, List<TaskDto>> tasksByDay = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .filter(task -> task.getCompletedAt() != null)
                .collect(Collectors.groupingBy(task -> task.getCompletedAt().getDayOfWeek()));
        
        Map<DayOfWeek, Double> dayProductivity = new HashMap<>();
        
        for (DayOfWeek day : DayOfWeek.values()) {
            List<TaskDto> dayTasks = tasksByDay.getOrDefault(day, Collections.emptyList());
            double productivity = calculateDayProductivity(dayTasks);
            dayProductivity.put(day, productivity);
        }
        
        return dayProductivity;
    }

    private double calculateDayProductivity(List<TaskDto> dayTasks) {
        if (dayTasks.isEmpty()) {
            return 5.0; // Default neutral score
        }
        
        // Calculate based on task completion count and priority
        double baseScore = Math.min(10.0, dayTasks.size() * 1.5);
        
        // Bonus for high priority tasks
        long highPriorityCount = dayTasks.stream()
                .filter(task -> "HIGH".equals(task.getPriority()) || "CRITICAL".equals(task.getPriority()))
                .count();
        
        double priorityBonus = highPriorityCount * 0.5;
        
        return Math.min(10.0, baseScore + priorityBonus);
    }

    private Map<String, Double> analyzeWeeklyTrends(List<TaskDto> tasks) {
        Map<String, Double> trends = new HashMap<>();
        
        // Group tasks by week
        Map<Integer, List<TaskDto>> tasksByWeek = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .filter(task -> task.getCompletedAt() != null)
                .collect(Collectors.groupingBy(task -> 
                    (int) ChronoUnit.WEEKS.between(LocalDate.now().minusWeeks(12), task.getCompletedAt().toLocalDate())));
        
        List<Double> weeklyScores = tasksByWeek.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> calculateWeeklyProductivityScore(entry.getValue()))
                .collect(Collectors.toList());
        
        if (weeklyScores.size() >= 2) {
            double recentTrend = calculateTrendSlope(weeklyScores);
            trends.put("recentTrend", recentTrend);
            trends.put("averageWeeklyScore", weeklyScores.stream().mapToDouble(Double::doubleValue).average().orElse(5.0));
        } else {
            trends.put("recentTrend", 0.0);
            trends.put("averageWeeklyScore", 5.0);
        }
        
        return trends;
    }

    private double calculateWeeklyProductivityScore(List<TaskDto> weekTasks) {
        if (weekTasks.isEmpty()) return 0.0;
        
        double taskCount = weekTasks.size();
        double priorityWeight = weekTasks.stream()
                .mapToDouble(task -> getPriorityWeight(task.getPriority()))
                .average()
                .orElse(1.0);
        
        return Math.min(10.0, taskCount * priorityWeight);
    }

    private double getPriorityWeight(String priority) {
        switch (priority != null ? priority.toUpperCase() : "MEDIUM") {
            case "CRITICAL": return 2.0;
            case "HIGH": return 1.5;
            case "MEDIUM": return 1.0;
            case "LOW": return 0.7;
            default: return 1.0;
        }
    }

    private double calculateTrendSlope(List<Double> values) {
        if (values.size() < 2) return 0.0;
        
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }

    private double calculateAverageVelocity(List<TaskDto> tasks) {
        List<TaskDto> completedTasks = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .filter(task -> task.getCompletedAt() != null)
                .collect(Collectors.toList());
        
        if (completedTasks.isEmpty()) return 1.0;
        
        // Calculate tasks per day
        LocalDate earliest = completedTasks.stream()
                .map(task -> task.getCompletedAt().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now().minusDays(7));
        
        LocalDate latest = completedTasks.stream()
                .map(task -> task.getCompletedAt().toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
        
        long daysBetween = ChronoUnit.DAYS.between(earliest, latest);
        if (daysBetween == 0) daysBetween = 1;
        
        return (double) completedTasks.size() / daysBetween;
    }

    private double calculateConsistencyScore(List<TaskDto> tasks) {
        Map<LocalDate, Long> dailyCompletions = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .filter(task -> task.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        task -> task.getCompletedAt().toLocalDate(),
                        Collectors.counting()
                ));
        
        if (dailyCompletions.size() < 3) return 0.5;
        
        double mean = dailyCompletions.values().stream().mapToDouble(Long::doubleValue).average().orElse(0.0);
        double variance = dailyCompletions.values().stream()
                .mapToDouble(val -> Math.pow(val - mean, 2))
                .average()
                .orElse(0.0);
        
        if (mean == 0) return 0.0;
        
        double coefficientOfVariation = Math.sqrt(variance) / mean;
        return Math.max(0.0, 1.0 - coefficientOfVariation);
    }

    private List<DailyProductivityPrediction> generateDailyPredictions(
            Map<String, Object> historicalAnalysis, 
            TrendAnalysis trendAnalysis, 
            int forecastDays) {
        
        List<DailyProductivityPrediction> predictions = new ArrayList<>();
        LocalDate currentDate = LocalDate.now().plusDays(1);
        
        @SuppressWarnings("unchecked")
        Map<DayOfWeek, Double> dayPatterns = (Map<DayOfWeek, Double>) historicalAnalysis.get("dayOfWeekPatterns");
        double averageVelocity = (Double) historicalAnalysis.get("averageVelocity");
        double consistencyScore = (Double) historicalAnalysis.get("consistencyScore");
        
        for (int i = 0; i < forecastDays; i++) {
            LocalDate predictionDate = currentDate.plusDays(i);
            DayOfWeek dayOfWeek = predictionDate.getDayOfWeek();
            
            // Base prediction from day-of-week patterns
            double basePrediction = dayPatterns.getOrDefault(dayOfWeek, 5.0);
            
            // Apply trend adjustment
            double trendAdjustment = calculateTrendAdjustment(trendAnalysis, i);
            double adjustedPrediction = basePrediction + trendAdjustment;
            
            // Calculate confidence based on historical data quality
            double dayConfidence = calculateDayConfidence(consistencyScore, i);
            
            // Calculate confidence intervals
            double uncertaintyRange = (1.0 - dayConfidence) * 2.0;
            double lowerBound = Math.max(0.0, adjustedPrediction - uncertaintyRange);
            double upperBound = Math.min(10.0, adjustedPrediction + uncertaintyRange);
            
            // Estimate task completion metrics
            int expectedTasks = (int) Math.round(averageVelocity * (adjustedPrediction / 5.0));
            double expectedCompletionRate = Math.min(100.0, (adjustedPrediction / 10.0) * 100.0);
            
            predictions.add(DailyProductivityPrediction.builder()
                    .date(predictionDate)
                    .predictedScore(Math.max(0.0, Math.min(10.0, adjustedPrediction)))
                    .confidence(dayConfidence)
                    .lowerBound(lowerBound)
                    .upperBound(upperBound)
                    .expectedTasksCompleted(Math.max(0, expectedTasks))
                    .expectedCompletionRate(expectedCompletionRate)
                    .dayOfWeekPattern(getDayOfWeekPattern(dayOfWeek))
                    .reasoning(generatePredictionReasoning(dayOfWeek, basePrediction, trendAdjustment))
                    .build());
        }
        
        return predictions;
    }

    private double calculateTrendAdjustment(TrendAnalysis trendAnalysis, int dayOffset) {
        if (trendAnalysis == null || trendAnalysis.getTrendStrength() == 0) {
            return 0.0;
        }
        
        double trendDirection = "INCREASING".equals(trendAnalysis.getTrendDirection()) ? 1.0 : 
                               "DECREASING".equals(trendAnalysis.getTrendDirection()) ? -1.0 : 0.0;
        
        // Diminishing trend effect over time
        double timeDecay = Math.exp(-dayOffset * 0.1);
        return trendDirection * trendAnalysis.getTrendStrength() * timeDecay * 0.5;
    }

    private double calculateDayConfidence(double consistencyScore, int dayOffset) {
        // Confidence decreases with distance into future and lower consistency
        double baseConfidence = 0.8 * consistencyScore;
        double timeDecay = Math.exp(-dayOffset * 0.15);
        return Math.max(0.3, baseConfidence * timeDecay);
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

    private String generatePredictionReasoning(DayOfWeek dayOfWeek, double basePrediction, double trendAdjustment) {
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("Based on ").append(dayOfWeek.name().toLowerCase()).append(" patterns");
        
        if (Math.abs(trendAdjustment) > 0.1) {
            reasoning.append(" with ").append(trendAdjustment > 0 ? "positive" : "negative").append(" trend adjustment");
        }
        
        if (basePrediction > 7.0) {
            reasoning.append(". Historically high productivity day");
        } else if (basePrediction < 4.0) {
            reasoning.append(". Historically lower productivity day");
        }
        
        return reasoning.toString();
    }

    private double calculateOverallForecastScore(List<DailyProductivityPrediction> predictions) {
        return predictions.stream()
                .mapToDouble(DailyProductivityPrediction::getPredictedScore)
                .average()
                .orElse(5.0);
    }

    private double calculateForecastConfidence(List<TaskDto> historicalTasks, TrendAnalysis trendAnalysis) {
        // Base confidence on data quality
        int dataPoints = historicalTasks.size();
        double dataQualityScore = Math.min(1.0, dataPoints / 30.0);
        
        // Factor in trend analysis confidence
        double trendConfidence = trendAnalysis != null ? trendAnalysis.getConfidence() : 0.5;
        
        // Historical data recency
        long daysSinceLastTask = historicalTasks.stream()
                .filter(task -> task.getCompletedAt() != null)
                .mapToLong(task -> ChronoUnit.DAYS.between(task.getCompletedAt().toLocalDate(), LocalDate.now()))
                .min()
                .orElse(30);
        
        double recencyScore = Math.max(0.3, 1.0 - (daysSinceLastTask / 30.0));
        
        double overallConfidence = (dataQualityScore * 0.4 + trendConfidence * 0.4 + recencyScore * 0.2);
        
        if (overallConfidence >= HIGH_CONFIDENCE_THRESHOLD) {
            return overallConfidence;
        } else if (overallConfidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
            return overallConfidence;
        } else {
            return Math.max(0.3, overallConfidence);
        }
    }

    private Map<String, Object> createForecastMetadata(Map<String, Object> historicalAnalysis, TrendAnalysis trendAnalysis) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("historicalDataPoints", historicalAnalysis.size());
        metadata.put("trendDirection", trendAnalysis != null ? trendAnalysis.getTrendDirection() : "STABLE");
        metadata.put("trendStrength", trendAnalysis != null ? trendAnalysis.getTrendStrength() : 0.0);
        metadata.put("forecastAlgorithm", "PATTERN_BASED_WITH_TREND_ADJUSTMENT");
        metadata.put("generationTime", LocalDateTime.now());
        return metadata;
    }

    private List<String> createForecastAssumptions() {
        return Arrays.asList(
                "Historical patterns will continue with similar consistency",
                "No major changes in work schedule or priorities",
                "Current trend direction will gradually diminish over time",
                "Day-of-week patterns remain stable",
                "External factors remain constant"
        );
    }

    private double calculateUncertaintyRange(double confidence) {
        // Higher confidence = lower uncertainty range
        return (1.0 - confidence) * 3.0; // Max uncertainty of 3 points on 10-point scale
    }

    private ProductivityForecast createEmptyForecast(String userId, int forecastDays) {
        List<DailyProductivityPrediction> emptyPredictions = new ArrayList<>();
        LocalDate currentDate = LocalDate.now().plusDays(1);
        
        for (int i = 0; i < forecastDays; i++) {
            LocalDate predictionDate = currentDate.plusDays(i);
            emptyPredictions.add(DailyProductivityPrediction.builder()
                    .date(predictionDate)
                    .predictedScore(5.0) // Neutral prediction
                    .confidence(0.3) // Low confidence
                    .lowerBound(3.0)
                    .upperBound(7.0)
                    .expectedTasksCompleted(1)
                    .expectedCompletionRate(50.0)
                    .dayOfWeekPattern(getDayOfWeekPattern(predictionDate.getDayOfWeek()))
                    .reasoning("Insufficient historical data for accurate prediction")
                    .build());
        }
        
        return ProductivityForecast.builder()
                .userId(userId)
                .dailyPredictions(emptyPredictions)
                .overallForecastScore(5.0)
                .confidence(0.3)
                .forecastMethod("DEFAULT_BASELINE")
                .generatedAt(LocalDateTime.now())
                .forecastStartDate(currentDate)
                .forecastEndDate(currentDate.plusDays(forecastDays - 1))
                .metadata(Map.of("reason", "INSUFFICIENT_DATA"))
                .assumptions(Arrays.asList("No historical data available", "Using baseline predictions"))
                .uncertaintyRange(2.0)
                .build();
    }
}