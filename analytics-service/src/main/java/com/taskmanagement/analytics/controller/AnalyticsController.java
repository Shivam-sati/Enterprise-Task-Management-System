package com.taskmanagement.analytics.controller;

import com.taskmanagement.analytics.dto.TaskDto;
import com.taskmanagement.analytics.model.*;
import com.taskmanagement.analytics.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsCalculator analyticsCalculator;
    private final DataAggregator dataAggregator;
    private final TrendAnalysisEngine trendAnalysisEngine;
    private final ProductivityPredictor productivityPredictor;
    private final ProactiveAlertSystem proactiveAlertSystem;
    private final TaskServiceClient taskServiceClient;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Analytics Service",
            "message", "Analytics Service is running successfully"
        ));
    }

    @GetMapping("/productivity/{userId}")
    public ResponseEntity<Map<String, Object>> getProductivityMetrics(
            @PathVariable String userId,
            @RequestParam(defaultValue = "30") int days) {
        log.info("Getting productivity metrics for user: {} over {} days", userId, days);
        
        try {
            Period period = Period.ofDays(days);
            List<TaskDto> tasks = taskServiceClient.getTasksForUser(userId, period);
            ProductivityMetrics metrics = analyticsCalculator.calculateProductivity(userId, tasks, period);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", metrics.getUserId());
            response.put("completionRate", metrics.getCompletionRate());
            response.put("averageTaskTime", metrics.getAverageTaskTime());
            response.put("tasksCompleted", metrics.getTasksCompleted());
            response.put("tasksCreated", metrics.getTasksCreated());
            response.put("productivityScore", metrics.getProductivityScore());
            response.put("period", String.format("last_%d_days", days));
            response.put("calculatedAt", metrics.getCalculatedAt());
            
            // Enhanced metadata
            response.put("confidence", metrics.getConfidence());
            response.put("dataQuality", metrics.getDataQuality());
            response.put("breakdown", metrics.getBreakdown());
            
            // Data source information
            response.put("dataSource", "REAL_TASK_DATA");
            response.put("calculationMethod", "ANALYTICS_CALCULATOR");
            
            // Warnings for insufficient data
            if ("INSUFFICIENT_DATA".equals(metrics.getDataQuality()) || "NO_DATA".equals(metrics.getDataQuality())) {
                response.put("warning", "Insufficient data for reliable metrics. Complete more tasks to improve accuracy.");
            } else if ("LOW_QUALITY".equals(metrics.getDataQuality())) {
                response.put("warning", "Limited data available. Metrics may not be fully representative.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error calculating productivity metrics for user: {}", userId, e);
            
            // Fallback response with error information
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("userId", userId);
            errorResponse.put("error", "Unable to calculate real metrics");
            errorResponse.put("errorMessage", e.getMessage());
            errorResponse.put("dataSource", "ERROR_FALLBACK");
            errorResponse.put("calculatedAt", LocalDateTime.now());
            
            // Provide basic fallback values
            errorResponse.put("completionRate", 0.0);
            errorResponse.put("averageTaskTime", 0.0);
            errorResponse.put("tasksCompleted", 0);
            errorResponse.put("tasksCreated", 0);
            errorResponse.put("productivityScore", 0.0);
            errorResponse.put("confidence", 0.0);
            errorResponse.put("dataQuality", "ERROR");
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/dashboard/{userId}")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @PathVariable String userId,
            @RequestParam(defaultValue = "30") int days) {
        log.info("Getting dashboard data for user: {} over {} days", userId, days);
        
        try {
            Period period = Period.ofDays(days);
            TaskStatistics statistics = dataAggregator.aggregateTaskData(userId, period);
            
            Map<String, Object> response = new HashMap<>();
            
            // Basic task counts
            response.put("totalTasks", statistics.getTotalTasks());
            response.put("completedTasks", statistics.getCompletedTasks());
            response.put("pendingTasks", statistics.getPendingTasks());
            response.put("overdueTasks", statistics.getOverdueTasks());
            response.put("cancelledTasks", statistics.getCancelledTasks());
            
            // Weekly progress from daily counts
            List<Integer> weeklyProgress = statistics.getDailyCounts().stream()
                    .mapToInt(DailyTaskCount::getCompleted)
                    .boxed()
                    .toList();
            response.put("weeklyProgress", weeklyProgress);
            
            // Top categories from task breakdown
            Map<String, Integer> categoryBreakdown = statistics.getTasksByCategory();
            List<Map<String, Object>> topCategories = categoryBreakdown.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .map(entry -> {
                        Map<String, Object> categoryMap = new HashMap<>();
                        categoryMap.put("name", entry.getKey());
                        categoryMap.put("count", entry.getValue());
                        return categoryMap;
                    })
                    .collect(Collectors.toList());
            response.put("topCategories", topCategories);
            
            // Time analysis
            Map<String, Object> timeSpent = new HashMap<>();
            timeSpent.put("totalHours", statistics.getTotalTimeSpent());
            timeSpent.put("averagePerTask", statistics.getAverageCompletionTime());
            timeSpent.put("period", String.format("%d days", days));
            response.put("timeSpent", timeSpent);
            
            // Enhanced metadata
            response.put("calculatedAt", statistics.getCalculatedAt());
            response.put("periodStart", statistics.getPeriodStart());
            response.put("periodEnd", statistics.getPeriodEnd());
            response.put("dataSource", "REAL_TASK_DATA");
            
            // Data quality indicators
            if (statistics.getTotalTasks() == 0) {
                response.put("warning", "No tasks found for the specified period");
                response.put("dataQuality", "NO_DATA");
            } else if (statistics.getTotalTasks() < 5) {
                response.put("warning", "Limited task data available. Dashboard may not be fully representative.");
                response.put("dataQuality", "LOW_QUALITY");
            } else {
                response.put("dataQuality", "SUFFICIENT");
            }
            
            // Priority breakdown
            response.put("priorityBreakdown", statistics.getTasksByPriority());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating dashboard data for user: {}", userId, e);
            
            // Fallback response with error information
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("userId", userId);
            errorResponse.put("error", "Unable to generate real dashboard data");
            errorResponse.put("errorMessage", e.getMessage());
            errorResponse.put("dataSource", "ERROR_FALLBACK");
            errorResponse.put("calculatedAt", LocalDateTime.now());
            
            // Provide empty fallback values
            errorResponse.put("totalTasks", 0);
            errorResponse.put("completedTasks", 0);
            errorResponse.put("pendingTasks", 0);
            errorResponse.put("overdueTasks", 0);
            errorResponse.put("weeklyProgress", List.of());
            errorResponse.put("topCategories", List.of());
            errorResponse.put("timeSpent", Map.of("totalHours", 0.0, "averagePerTask", 0.0));
            errorResponse.put("dataQuality", "ERROR");
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/trends/{userId}")
    public ResponseEntity<Map<String, Object>> getTrends(
            @PathVariable String userId,
            @RequestParam(defaultValue = "30") int days) {
        log.info("Getting trends for user: {} over {} days", userId, days);
        
        try {
            Period period = Period.ofDays(days);
            TrendAnalysis trendAnalysis = trendAnalysisEngine.analyzeTrends(userId, period);
            
            Map<String, Object> response = new HashMap<>();
            
            // Completion trends
            List<Map<String, Object>> completionTrend = trendAnalysis.getCompletionTrends().stream()
                    .map(trend -> {
                        Map<String, Object> trendMap = new HashMap<>();
                        trendMap.put("date", trend.getDate().toString());
                        trendMap.put("completed", trend.getCompleted());
                        trendMap.put("created", trend.getCreated());
                        trendMap.put("completionRate", trend.getCompletionRate());
                        trendMap.put("hoursSpent", trend.getHoursSpent());
                        trendMap.put("productivity", trend.getProductivity());
                        return trendMap;
                    })
                    .collect(Collectors.toList());
            response.put("completionTrend", completionTrend);
            
            // Productivity patterns
            ProductivityPattern pattern = trendAnalysis.getProductivityPattern();
            Map<String, Object> productivityPattern = new HashMap<>();
            productivityPattern.put("bestHour", pattern.getBestHour());
            productivityPattern.put("bestDay", pattern.getBestDay().toString());
            productivityPattern.put("averageSessionTime", pattern.getAverageSessionTime());
            productivityPattern.put("workingPattern", pattern.getWorkingPattern());
            productivityPattern.put("consistency", pattern.getConsistency());
            productivityPattern.put("dailyProductivity", pattern.getDailyProductivity());
            productivityPattern.put("hourlyProductivity", pattern.getHourlyProductivity());
            response.put("productivityPattern", productivityPattern);
            
            // Insights and performance metrics
            response.put("insights", trendAnalysis.getInsights());
            response.put("performanceMetrics", trendAnalysis.getPerformanceMetrics());
            
            // Enhanced metadata
            response.put("analyzedAt", trendAnalysis.getAnalyzedAt());
            response.put("confidence", trendAnalysis.getConfidence());
            response.put("trendDirection", trendAnalysis.getTrendDirection());
            response.put("trendStrength", trendAnalysis.getTrendStrength());
            response.put("dataSource", "REAL_TASK_DATA");
            response.put("analysisMethod", "TREND_ANALYSIS_ENGINE");
            
            // Data quality indicators and warnings
            if (trendAnalysis.getConfidence() < 0.5) {
                response.put("warning", "Low confidence in trend analysis due to insufficient data");
                response.put("dataQuality", "LOW_CONFIDENCE");
            } else if (trendAnalysis.getConfidence() < 0.7) {
                response.put("warning", "Moderate confidence in trend analysis. More data will improve accuracy");
                response.put("dataQuality", "MEDIUM_CONFIDENCE");
            } else {
                response.put("dataQuality", "HIGH_CONFIDENCE");
            }
            
            if ("INSUFFICIENT_DATA".equals(trendAnalysis.getTrendDirection())) {
                response.put("warning", "Insufficient data for reliable trend analysis. Complete more tasks over time to see patterns.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error analyzing trends for user: {}", userId, e);
            
            // Fallback response with error information
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("userId", userId);
            errorResponse.put("error", "Unable to analyze trends");
            errorResponse.put("errorMessage", e.getMessage());
            errorResponse.put("dataSource", "ERROR_FALLBACK");
            errorResponse.put("analyzedAt", LocalDateTime.now());
            
            // Provide empty fallback values
            errorResponse.put("completionTrend", List.of());
            errorResponse.put("productivityPattern", Map.of(
                    "bestHour", 9,
                    "bestDay", "MONDAY",
                    "averageSessionTime", 0.0,
                    "workingPattern", "UNKNOWN",
                    "consistency", 0.0
            ));
            errorResponse.put("insights", List.of("Unable to generate insights due to data error"));
            errorResponse.put("performanceMetrics", Map.of());
            errorResponse.put("confidence", 0.0);
            errorResponse.put("trendDirection", "ERROR");
            errorResponse.put("trendStrength", 0.0);
            errorResponse.put("dataQuality", "ERROR");
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/forecast/{userId}")
    public ResponseEntity<Map<String, Object>> getProductivityForecast(
            @PathVariable String userId,
            @RequestParam(defaultValue = "7") int forecastDays,
            @RequestParam(defaultValue = "30") int historicalDays) {
        log.info("Getting {}-day productivity forecast for user: {} based on {} days of history", 
                forecastDays, userId, historicalDays);
        
        try {
            Period historicalPeriod = Period.ofDays(historicalDays);
            List<TaskDto> historicalTasks = taskServiceClient.getTasksForUser(userId, historicalPeriod);
            ProductivityForecast forecast = productivityPredictor.predictProductivity(userId, historicalTasks, forecastDays);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", forecast.getUserId());
            response.put("overallForecastScore", forecast.getOverallForecastScore());
            response.put("confidence", forecast.getConfidence());
            response.put("forecastMethod", forecast.getForecastMethod());
            response.put("generatedAt", forecast.getGeneratedAt());
            response.put("forecastStartDate", forecast.getForecastStartDate());
            response.put("forecastEndDate", forecast.getForecastEndDate());
            response.put("uncertaintyRange", forecast.getUncertaintyRange());
            
            // Daily predictions
            List<Map<String, Object>> dailyPredictions = forecast.getDailyPredictions().stream()
                    .map(pred -> {
                        Map<String, Object> predMap = new HashMap<>();
                        predMap.put("date", pred.getDate().toString());
                        predMap.put("predictedScore", pred.getPredictedScore());
                        predMap.put("confidence", pred.getConfidence());
                        predMap.put("lowerBound", pred.getLowerBound());
                        predMap.put("upperBound", pred.getUpperBound());
                        predMap.put("expectedTasksCompleted", pred.getExpectedTasksCompleted());
                        predMap.put("expectedCompletionRate", pred.getExpectedCompletionRate());
                        predMap.put("dayOfWeekPattern", pred.getDayOfWeekPattern());
                        predMap.put("reasoning", pred.getReasoning());
                        return predMap;
                    })
                    .collect(Collectors.toList());
            response.put("dailyPredictions", dailyPredictions);
            
            // Metadata and assumptions
            response.put("metadata", forecast.getMetadata());
            response.put("assumptions", forecast.getAssumptions());
            response.put("dataSource", "PREDICTIVE_ANALYTICS");
            
            // Data quality warnings
            if (forecast.getConfidence() < 0.5) {
                response.put("warning", "Low confidence forecast due to insufficient historical data");
                response.put("dataQuality", "LOW_CONFIDENCE");
            } else if (forecast.getConfidence() < 0.7) {
                response.put("dataQuality", "MEDIUM_CONFIDENCE");
            } else {
                response.put("dataQuality", "HIGH_CONFIDENCE");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating productivity forecast for user: {}", userId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("userId", userId);
            errorResponse.put("error", "Unable to generate productivity forecast");
            errorResponse.put("errorMessage", e.getMessage());
            errorResponse.put("dataSource", "ERROR_FALLBACK");
            errorResponse.put("generatedAt", LocalDateTime.now());
            errorResponse.put("confidence", 0.0);
            errorResponse.put("dataQuality", "ERROR");
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/alerts/{userId}")
    public ResponseEntity<Map<String, Object>> getProactiveAlerts(
            @PathVariable String userId,
            @RequestParam(defaultValue = "MEDIUM") String sensitivity,
            @RequestParam(defaultValue = "30") int historicalDays) {
        log.info("Getting proactive alerts for user: {} with sensitivity: {}", userId, sensitivity);
        
        try {
            Period historicalPeriod = Period.ofDays(historicalDays);
            List<TaskDto> historicalTasks = taskServiceClient.getTasksForUser(userId, historicalPeriod);
            
            AlertThreshold.Sensitivity alertSensitivity = 
                    AlertThreshold.Sensitivity.valueOf(sensitivity.toUpperCase());
            
            List<ProactiveAlert> alerts = proactiveAlertSystem.generateProactiveAlerts(
                    userId, historicalTasks, alertSensitivity);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("alertCount", alerts.size());
            response.put("generatedAt", LocalDateTime.now());
            response.put("sensitivity", sensitivity);
            
            // Convert alerts to response format
            List<Map<String, Object>> alertsData = alerts.stream()
                    .map(alert -> {
                        Map<String, Object> alertMap = new HashMap<>();
                        alertMap.put("alertId", alert.getAlertId());
                        alertMap.put("type", alert.getType().toString());
                        alertMap.put("severity", alert.getSeverity().toString());
                        alertMap.put("title", alert.getTitle());
                        alertMap.put("message", alert.getMessage());
                        alertMap.put("recommendation", alert.getRecommendation());
                        alertMap.put("confidence", alert.getConfidence());
                        alertMap.put("triggeredAt", alert.getTriggeredAt());
                        alertMap.put("expiresAt", alert.getExpiresAt());
                        alertMap.put("acknowledged", alert.isAcknowledged());
                        alertMap.put("context", alert.getContext());
                        alertMap.put("actionItems", alert.getActionItems());
                        alertMap.put("triggerReason", alert.getTriggerReason());
                        return alertMap;
                    })
                    .toList();
            response.put("alerts", alertsData);
            
            // Summary by severity
            Map<String, Long> severityCounts = alerts.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            alert -> alert.getSeverity().toString(),
                            java.util.stream.Collectors.counting()
                    ));
            response.put("severityBreakdown", severityCounts);
            
            response.put("dataSource", "PROACTIVE_ALERT_SYSTEM");
            
            // Data quality indicators
            if (alerts.isEmpty()) {
                response.put("message", "No alerts generated - productivity patterns appear normal");
                response.put("dataQuality", "NORMAL");
            } else {
                response.put("dataQuality", "ALERTS_GENERATED");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating proactive alerts for user: {}", userId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("userId", userId);
            errorResponse.put("error", "Unable to generate proactive alerts");
            errorResponse.put("errorMessage", e.getMessage());
            errorResponse.put("dataSource", "ERROR_FALLBACK");
            errorResponse.put("generatedAt", LocalDateTime.now());
            errorResponse.put("alertCount", 0);
            errorResponse.put("alerts", List.of());
            errorResponse.put("dataQuality", "ERROR");
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    @PostMapping("/track-activity")
    public ResponseEntity<Map<String, String>> trackActivity(@RequestBody Map<String, Object> activity) {
        log.info("Tracking activity: {}", activity);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Activity tracked successfully",
            "activityId", "act_" + System.currentTimeMillis()
        ));
    }

    /**
     * Enhanced response builder with comprehensive metadata
     */
    private Map<String, Object> buildEnhancedResponse(String userId, Object data, String dataSource, String calculationMethod) {
        Map<String, Object> response = new HashMap<>();
        
        if (data instanceof Map) {
            response.putAll((Map<String, Object>) data);
        } else {
            response.put("data", data);
        }
        
        // Standard metadata
        response.put("userId", userId);
        response.put("timestamp", LocalDateTime.now());
        response.put("dataSource", dataSource);
        response.put("calculationMethod", calculationMethod);
        response.put("version", "1.0");
        
        return response;
    }

    /**
     * Add data quality indicators and warnings to response
     */
    private void addDataQualityMetadata(Map<String, Object> response, int dataPoints, double confidence, String dataQuality) {
        Map<String, Object> qualityMetadata = new HashMap<>();
        qualityMetadata.put("dataPoints", dataPoints);
        qualityMetadata.put("confidence", confidence);
        qualityMetadata.put("quality", dataQuality);
        qualityMetadata.put("assessedAt", LocalDateTime.now());
        
        response.put("dataQuality", qualityMetadata);
        
        // Add appropriate warnings
        if (dataPoints == 0) {
            response.put("warning", "No data available for analysis");
            response.put("recommendation", "Complete some tasks to enable analytics");
        } else if (dataPoints < 5) {
            response.put("warning", "Very limited data available - results may not be representative");
            response.put("recommendation", "Complete more tasks to improve accuracy of analytics");
        } else if (confidence < 0.5) {
            response.put("warning", "Low confidence in results due to data quality issues");
            response.put("recommendation", "Ensure consistent task completion and data entry");
        } else if (confidence < 0.7) {
            response.put("info", "Moderate confidence in results - more data will improve accuracy");
        }
    }

    /**
     * Build error response with comprehensive error information
     */
    private Map<String, Object> buildErrorResponse(String userId, String operation, Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("userId", userId);
        errorResponse.put("operation", operation);
        errorResponse.put("status", "ERROR");
        errorResponse.put("error", "Operation failed");
        errorResponse.put("errorMessage", e.getMessage());
        errorResponse.put("errorType", e.getClass().getSimpleName());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("dataSource", "ERROR_FALLBACK");
        
        // Add troubleshooting information
        Map<String, Object> troubleshooting = new HashMap<>();
        troubleshooting.put("possibleCauses", List.of(
                "Task service unavailable",
                "Database connection issues", 
                "Insufficient data for calculation",
                "Invalid user ID or parameters"
        ));
        troubleshooting.put("recommendations", List.of(
                "Check if task service is running",
                "Verify user has created tasks",
                "Try again in a few moments",
                "Contact support if problem persists"
        ));
        errorResponse.put("troubleshooting", troubleshooting);
        
        return errorResponse;
    }
}