package com.taskmanagement.analytics.service;

import com.taskmanagement.analytics.dto.TaskDto;
import com.taskmanagement.analytics.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProactiveAlertSystem {

    private final ProductivityPredictor productivityPredictor;
    private final TrendAnalysisEngine trendAnalysisEngine;

    private static final Map<ProactiveAlert.AlertType, AlertThreshold> DEFAULT_THRESHOLDS = createDefaultThresholds();

    /**
     * Generate proactive alerts based on productivity predictions and trends
     */
    public List<ProactiveAlert> generateProactiveAlerts(String userId, List<TaskDto> historicalTasks) {
        return generateProactiveAlerts(userId, historicalTasks, AlertThreshold.Sensitivity.MEDIUM);
    }

    /**
     * Generate proactive alerts with configurable sensitivity
     */
    public List<ProactiveAlert> generateProactiveAlerts(String userId, List<TaskDto> historicalTasks, 
                                                       AlertThreshold.Sensitivity sensitivity) {
        log.info("Generating proactive alerts for user: {} with sensitivity: {}", userId, sensitivity);
        
        List<ProactiveAlert> alerts = new ArrayList<>();
        
        if (historicalTasks.isEmpty()) {
            return alerts; // No alerts without data
        }

        try {
            // Generate productivity forecast
            ProductivityForecast forecast = productivityPredictor.predictProductivity(userId, historicalTasks);
            
            // Analyze current trends
            TrendAnalysis trendAnalysis = trendAnalysisEngine.analyzeTrends(userId, historicalTasks);
            
            // Check for productivity drop predictions
            alerts.addAll(checkProductivityDropAlerts(userId, forecast, sensitivity));
            
            // Check for trend decline alerts
            alerts.addAll(checkTrendDeclineAlerts(userId, trendAnalysis, sensitivity));
            
            // Check for pattern anomalies
            alerts.addAll(checkPatternAnomalyAlerts(userId, historicalTasks, forecast, sensitivity));
            
            // Check for workload imbalance
            alerts.addAll(checkWorkloadImbalanceAlerts(userId, historicalTasks, sensitivity));
            
            // Check for burnout risk
            alerts.addAll(checkBurnoutRiskAlerts(userId, historicalTasks, trendAnalysis, sensitivity));
            
            // Check for efficiency opportunities
            alerts.addAll(checkEfficiencyOpportunityAlerts(userId, historicalTasks, forecast, sensitivity));
            
            // Prioritize and filter alerts
            alerts = prioritizeAndFilterAlerts(alerts);
            
            log.info("Generated {} proactive alerts for user: {}", alerts.size(), userId);
            
        } catch (Exception e) {
            log.error("Error generating proactive alerts for user: {}", userId, e);
        }
        
        return alerts;
    }

    private List<ProactiveAlert> checkProductivityDropAlerts(String userId, ProductivityForecast forecast, 
                                                           AlertThreshold.Sensitivity sensitivity) {
        List<ProactiveAlert> alerts = new ArrayList<>();
        
        if (forecast.getConfidence() < 0.5) {
            return alerts; // Skip if forecast confidence is too low
        }
        
        AlertThreshold threshold = adjustThresholdForSensitivity(
            DEFAULT_THRESHOLDS.get(ProactiveAlert.AlertType.PRODUCTIVITY_DROP), sensitivity);
        
        // Check for predicted productivity drops
        List<DailyProductivityPrediction> lowProductivityDays = forecast.getDailyPredictions().stream()
                .filter(pred -> pred.getPredictedScore() < threshold.getWarningThreshold())
                .collect(Collectors.toList());
        
        if (!lowProductivityDays.isEmpty()) {
            ProactiveAlert.AlertSeverity severity = determineSeverity(lowProductivityDays, threshold);
            
            String message = String.format("Predicted productivity drop detected. %d upcoming days show below-average productivity (%.1f/10).",
                    lowProductivityDays.size(), 
                    lowProductivityDays.stream().mapToDouble(DailyProductivityPrediction::getPredictedScore).average().orElse(0.0));
            
            alerts.add(ProactiveAlert.builder()
                    .alertId(generateAlertId())
                    .userId(userId)
                    .type(ProactiveAlert.AlertType.PRODUCTIVITY_DROP)
                    .severity(severity)
                    .title("Productivity Drop Predicted")
                    .message(message)
                    .recommendation(generateProductivityDropRecommendation(lowProductivityDays))
                    .confidence(forecast.getConfidence())
                    .triggeredAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .acknowledged(false)
                    .context(createProductivityDropContext(lowProductivityDays, forecast))
                    .actionItems(createProductivityDropActionItems(lowProductivityDays))
                    .triggerReason("Forecast shows productivity below threshold")
                    .build());
        }
        
        return alerts;
    }

    private List<ProactiveAlert> checkTrendDeclineAlerts(String userId, TrendAnalysis trendAnalysis, 
                                                       AlertThreshold.Sensitivity sensitivity) {
        List<ProactiveAlert> alerts = new ArrayList<>();
        
        if (trendAnalysis == null || trendAnalysis.getConfidence() < 0.5) {
            return alerts;
        }
        
        AlertThreshold threshold = adjustThresholdForSensitivity(
            DEFAULT_THRESHOLDS.get(ProactiveAlert.AlertType.TREND_DECLINE), sensitivity);
        
        // Check for declining trends
        if ("DECREASING".equals(trendAnalysis.getTrendDirection()) && 
            trendAnalysis.getTrendStrength() > threshold.getWarningThreshold()) {
            
            ProactiveAlert.AlertSeverity severity = trendAnalysis.getTrendStrength() > threshold.getCriticalThreshold() ? 
                    ProactiveAlert.AlertSeverity.HIGH : ProactiveAlert.AlertSeverity.MEDIUM;
            
            alerts.add(ProactiveAlert.builder()
                    .alertId(generateAlertId())
                    .userId(userId)
                    .type(ProactiveAlert.AlertType.TREND_DECLINE)
                    .severity(severity)
                    .title("Declining Productivity Trend")
                    .message(String.format("Your productivity has been declining with strength %.2f. " +
                            "This trend may continue if not addressed.", trendAnalysis.getTrendStrength()))
                    .recommendation("Consider reviewing your work patterns and identifying potential obstacles")
                    .confidence(trendAnalysis.getConfidence())
                    .triggeredAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(5))
                    .acknowledged(false)
                    .context(createTrendDeclineContext(trendAnalysis))
                    .actionItems(createTrendDeclineActionItems(trendAnalysis))
                    .triggerReason("Significant declining trend detected")
                    .build());
        }
        
        return alerts;
    }

    private List<ProactiveAlert> checkPatternAnomalyAlerts(String userId, List<TaskDto> historicalTasks, 
                                                         ProductivityForecast forecast, AlertThreshold.Sensitivity sensitivity) {
        List<ProactiveAlert> alerts = new ArrayList<>();
        
        AlertThreshold threshold = adjustThresholdForSensitivity(
            DEFAULT_THRESHOLDS.get(ProactiveAlert.AlertType.PATTERN_ANOMALY), sensitivity);
        
        // Check for unusual patterns in recent task completion
        Map<String, Object> recentPatterns = analyzeRecentPatterns(historicalTasks);
        double anomalyScore = calculateAnomalyScore(recentPatterns, forecast);
        
        if (anomalyScore > threshold.getWarningThreshold()) {
            ProactiveAlert.AlertSeverity severity = anomalyScore > threshold.getCriticalThreshold() ? 
                    ProactiveAlert.AlertSeverity.HIGH : ProactiveAlert.AlertSeverity.MEDIUM;
            
            alerts.add(ProactiveAlert.builder()
                    .alertId(generateAlertId())
                    .userId(userId)
                    .type(ProactiveAlert.AlertType.PATTERN_ANOMALY)
                    .severity(severity)
                    .title("Unusual Work Pattern Detected")
                    .message("Your recent work patterns differ significantly from your historical behavior")
                    .recommendation("Review recent changes in your work routine or environment")
                    .confidence(0.7)
                    .triggeredAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(3))
                    .acknowledged(false)
                    .context(recentPatterns)
                    .actionItems(createPatternAnomalyActionItems())
                    .triggerReason("Significant deviation from historical patterns")
                    .build());
        }
        
        return alerts;
    }

    private List<ProactiveAlert> checkWorkloadImbalanceAlerts(String userId, List<TaskDto> historicalTasks, 
                                                            AlertThreshold.Sensitivity sensitivity) {
        List<ProactiveAlert> alerts = new ArrayList<>();
        
        AlertThreshold threshold = adjustThresholdForSensitivity(
            DEFAULT_THRESHOLDS.get(ProactiveAlert.AlertType.WORKLOAD_IMBALANCE), sensitivity);
        
        // Analyze workload distribution
        Map<String, Object> workloadAnalysis = analyzeWorkloadDistribution(historicalTasks);
        double imbalanceScore = (Double) workloadAnalysis.getOrDefault("imbalanceScore", 0.0);
        
        if (imbalanceScore > threshold.getWarningThreshold()) {
            ProactiveAlert.AlertSeverity severity = imbalanceScore > threshold.getCriticalThreshold() ? 
                    ProactiveAlert.AlertSeverity.HIGH : ProactiveAlert.AlertSeverity.MEDIUM;
            
            alerts.add(ProactiveAlert.builder()
                    .alertId(generateAlertId())
                    .userId(userId)
                    .type(ProactiveAlert.AlertType.WORKLOAD_IMBALANCE)
                    .severity(severity)
                    .title("Workload Imbalance Detected")
                    .message("Your task distribution shows significant imbalance across priorities or time periods")
                    .recommendation("Consider redistributing tasks more evenly or adjusting priorities")
                    .confidence(0.8)
                    .triggeredAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .acknowledged(false)
                    .context(workloadAnalysis)
                    .actionItems(createWorkloadImbalanceActionItems(workloadAnalysis))
                    .triggerReason("Workload distribution exceeds balance threshold")
                    .build());
        }
        
        return alerts;
    }

    private List<ProactiveAlert> checkBurnoutRiskAlerts(String userId, List<TaskDto> historicalTasks, 
                                                      TrendAnalysis trendAnalysis, AlertThreshold.Sensitivity sensitivity) {
        List<ProactiveAlert> alerts = new ArrayList<>();
        
        AlertThreshold threshold = adjustThresholdForSensitivity(
            DEFAULT_THRESHOLDS.get(ProactiveAlert.AlertType.BURNOUT_RISK), sensitivity);
        
        double burnoutRisk = calculateBurnoutRisk(historicalTasks, trendAnalysis);
        
        if (burnoutRisk > threshold.getWarningThreshold()) {
            ProactiveAlert.AlertSeverity severity = burnoutRisk > threshold.getCriticalThreshold() ? 
                    ProactiveAlert.AlertSeverity.CRITICAL : ProactiveAlert.AlertSeverity.HIGH;
            
            alerts.add(ProactiveAlert.builder()
                    .alertId(generateAlertId())
                    .userId(userId)
                    .type(ProactiveAlert.AlertType.BURNOUT_RISK)
                    .severity(severity)
                    .title("Burnout Risk Detected")
                    .message("Current work patterns indicate elevated risk of burnout")
                    .recommendation("Consider taking breaks, reducing workload, or seeking support")
                    .confidence(0.75)
                    .triggeredAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .acknowledged(false)
                    .context(Map.of("burnoutRisk", burnoutRisk, "riskFactors", identifyBurnoutRiskFactors(historicalTasks)))
                    .actionItems(createBurnoutRiskActionItems())
                    .triggerReason("Multiple burnout risk indicators detected")
                    .build());
        }
        
        return alerts;
    }

    private List<ProactiveAlert> checkEfficiencyOpportunityAlerts(String userId, List<TaskDto> historicalTasks, 
                                                                ProductivityForecast forecast, AlertThreshold.Sensitivity sensitivity) {
        List<ProactiveAlert> alerts = new ArrayList<>();
        
        // Look for efficiency improvement opportunities
        List<String> opportunities = identifyEfficiencyOpportunities(historicalTasks, forecast);
        
        if (!opportunities.isEmpty() && sensitivity.getMultiplier() >= 0.5) {
            alerts.add(ProactiveAlert.builder()
                    .alertId(generateAlertId())
                    .userId(userId)
                    .type(ProactiveAlert.AlertType.EFFICIENCY_OPPORTUNITY)
                    .severity(ProactiveAlert.AlertSeverity.LOW)
                    .title("Efficiency Improvement Opportunities")
                    .message("Analysis suggests potential areas for productivity improvement")
                    .recommendation("Consider implementing suggested efficiency improvements")
                    .confidence(0.6)
                    .triggeredAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(14))
                    .acknowledged(false)
                    .context(Map.of("opportunities", opportunities))
                    .actionItems(opportunities)
                    .triggerReason("Efficiency improvement opportunities identified")
                    .build());
        }
        
        return alerts;
    }

    // Helper methods for alert generation
    private AlertThreshold adjustThresholdForSensitivity(AlertThreshold baseThreshold, AlertThreshold.Sensitivity sensitivity) {
        return AlertThreshold.builder()
                .thresholdId(baseThreshold.getThresholdId())
                .alertType(baseThreshold.getAlertType())
                .warningThreshold(baseThreshold.getWarningThreshold() * sensitivity.getMultiplier())
                .criticalThreshold(baseThreshold.getCriticalThreshold() * sensitivity.getMultiplier())
                .minimumDataPoints(baseThreshold.getMinimumDataPoints())
                .confidenceThreshold(baseThreshold.getConfidenceThreshold())
                .enabled(baseThreshold.isEnabled())
                .description(baseThreshold.getDescription())
                .build();
    }

    private ProactiveAlert.AlertSeverity determineSeverity(List<DailyProductivityPrediction> lowDays, AlertThreshold threshold) {
        double avgScore = lowDays.stream().mapToDouble(DailyProductivityPrediction::getPredictedScore).average().orElse(5.0);
        
        if (avgScore < threshold.getCriticalThreshold()) {
            return ProactiveAlert.AlertSeverity.HIGH;
        } else if (lowDays.size() >= 3) {
            return ProactiveAlert.AlertSeverity.MEDIUM;
        } else {
            return ProactiveAlert.AlertSeverity.LOW;
        }
    }

    private String generateProductivityDropRecommendation(List<DailyProductivityPrediction> lowDays) {
        StringBuilder rec = new StringBuilder("Consider: ");
        
        boolean hasWeekendDays = lowDays.stream().anyMatch(day -> 
            day.getDayOfWeekPattern().equals("WEEKEND"));
        boolean hasMondays = lowDays.stream().anyMatch(day -> 
            day.getDayOfWeekPattern().equals("WEEK_START"));
        
        if (hasMondays) {
            rec.append("preparing for Monday productivity, ");
        }
        if (hasWeekendDays) {
            rec.append("planning weekend work sessions, ");
        }
        
        rec.append("reviewing task priorities and breaking down complex tasks");
        return rec.toString();
    }

    private Map<String, Object> createProductivityDropContext(List<DailyProductivityPrediction> lowDays, 
                                                            ProductivityForecast forecast) {
        Map<String, Object> context = new HashMap<>();
        context.put("affectedDays", lowDays.size());
        context.put("averagePredictedScore", lowDays.stream().mapToDouble(DailyProductivityPrediction::getPredictedScore).average().orElse(0.0));
        context.put("forecastConfidence", forecast.getConfidence());
        context.put("dayPatterns", lowDays.stream().map(DailyProductivityPrediction::getDayOfWeekPattern).distinct().collect(Collectors.toList()));
        return context;
    }

    private List<String> createProductivityDropActionItems(List<DailyProductivityPrediction> lowDays) {
        List<String> actions = new ArrayList<>();
        actions.add("Review and prioritize upcoming tasks");
        actions.add("Break down complex tasks into smaller, manageable pieces");
        actions.add("Schedule important tasks for higher productivity periods");
        
        if (lowDays.stream().anyMatch(day -> day.getDayOfWeekPattern().equals("WEEK_START"))) {
            actions.add("Prepare Sunday evening for Monday productivity");
        }
        
        return actions;
    }

    private Map<String, Object> createTrendDeclineContext(TrendAnalysis trendAnalysis) {
        Map<String, Object> context = new HashMap<>();
        context.put("trendStrength", trendAnalysis.getTrendStrength());
        context.put("trendDirection", trendAnalysis.getTrendDirection());
        context.put("analysisConfidence", trendAnalysis.getConfidence());
        context.put("insights", trendAnalysis.getInsights());
        return context;
    }

    private List<String> createTrendDeclineActionItems(TrendAnalysis trendAnalysis) {
        List<String> actions = new ArrayList<>();
        actions.add("Identify factors contributing to declining productivity");
        actions.add("Review recent changes in work environment or routine");
        actions.add("Consider adjusting task scheduling or priorities");
        actions.add("Evaluate current workload and stress levels");
        return actions;
    }

    private Map<String, Object> analyzeRecentPatterns(List<TaskDto> tasks) {
        Map<String, Object> patterns = new HashMap<>();
        
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<TaskDto> recentTasks = tasks.stream()
                .filter(task -> task.getCreatedAt().isAfter(oneWeekAgo))
                .collect(Collectors.toList());
        
        // Calculate recent completion rate
        long completedRecent = recentTasks.stream().filter(task -> "COMPLETED".equals(task.getStatus())).count();
        double recentCompletionRate = recentTasks.isEmpty() ? 0.0 : (double) completedRecent / recentTasks.size();
        
        patterns.put("recentCompletionRate", recentCompletionRate);
        patterns.put("recentTaskCount", recentTasks.size());
        patterns.put("analysisDate", LocalDateTime.now());
        
        return patterns;
    }

    private double calculateAnomalyScore(Map<String, Object> recentPatterns, ProductivityForecast forecast) {
        double recentRate = (Double) recentPatterns.get("recentCompletionRate");
        double forecastAvg = forecast.getOverallForecastScore() / 10.0; // Convert to rate
        
        return Math.abs(recentRate - forecastAvg) * 2.0; // Scale to 0-2 range
    }

    private List<String> createPatternAnomalyActionItems() {
        return Arrays.asList(
                "Review recent changes in work schedule or environment",
                "Identify any new tools or processes that may be affecting productivity",
                "Consider returning to previously successful work patterns",
                "Monitor patterns for the next few days to confirm anomaly"
        );
    }

    private Map<String, Object> analyzeWorkloadDistribution(List<TaskDto> tasks) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Priority distribution
        Map<String, Long> priorityCounts = tasks.stream()
                .collect(Collectors.groupingBy(TaskDto::getPriority, Collectors.counting()));
        
        // Calculate imbalance score based on priority distribution
        long totalTasks = tasks.size();
        double imbalanceScore = 0.0;
        
        if (totalTasks > 0) {
            long highPriority = priorityCounts.getOrDefault("HIGH", 0L) + priorityCounts.getOrDefault("CRITICAL", 0L);
            double highPriorityRatio = (double) highPriority / totalTasks;
            
            // Imbalance if more than 70% or less than 10% high priority
            if (highPriorityRatio > 0.7 || highPriorityRatio < 0.1) {
                imbalanceScore = Math.abs(highPriorityRatio - 0.4) * 2.0; // Target 40% high priority
            }
        }
        
        analysis.put("imbalanceScore", imbalanceScore);
        analysis.put("priorityDistribution", priorityCounts);
        analysis.put("totalTasks", totalTasks);
        
        return analysis;
    }

    private List<String> createWorkloadImbalanceActionItems(Map<String, Object> workloadAnalysis) {
        List<String> actions = new ArrayList<>();
        actions.add("Review task priority distribution");
        actions.add("Consider redistributing high-priority tasks over time");
        actions.add("Evaluate if task priorities accurately reflect importance");
        actions.add("Balance urgent tasks with important long-term work");
        return actions;
    }

    private double calculateBurnoutRisk(List<TaskDto> tasks, TrendAnalysis trendAnalysis) {
        double riskScore = 0.0;
        
        // Factor 1: High task volume
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
        long recentTaskCount = tasks.stream()
                .filter(task -> task.getCreatedAt().isAfter(twoWeeksAgo))
                .count();
        
        if (recentTaskCount > 50) riskScore += 0.3;
        else if (recentTaskCount > 30) riskScore += 0.2;
        
        // Factor 2: Declining trend
        if (trendAnalysis != null && "DECREASING".equals(trendAnalysis.getTrendDirection())) {
            riskScore += trendAnalysis.getTrendStrength() * 0.4;
        }
        
        // Factor 3: High priority task ratio
        long highPriorityTasks = tasks.stream()
                .filter(task -> task.getCreatedAt().isAfter(twoWeeksAgo))
                .filter(task -> "HIGH".equals(task.getPriority()) || "CRITICAL".equals(task.getPriority()))
                .count();
        
        if (recentTaskCount > 0) {
            double highPriorityRatio = (double) highPriorityTasks / recentTaskCount;
            if (highPriorityRatio > 0.6) riskScore += 0.3;
        }
        
        return Math.min(1.0, riskScore);
    }

    private List<String> identifyBurnoutRiskFactors(List<TaskDto> tasks) {
        List<String> factors = new ArrayList<>();
        
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
        List<TaskDto> recentTasks = tasks.stream()
                .filter(task -> task.getCreatedAt().isAfter(twoWeeksAgo))
                .collect(Collectors.toList());
        
        if (recentTasks.size() > 50) {
            factors.add("High task volume in recent weeks");
        }
        
        long highPriorityCount = recentTasks.stream()
                .filter(task -> "HIGH".equals(task.getPriority()) || "CRITICAL".equals(task.getPriority()))
                .count();
        
        if (recentTasks.size() > 0 && (double) highPriorityCount / recentTasks.size() > 0.6) {
            factors.add("High proportion of urgent/critical tasks");
        }
        
        return factors;
    }

    private List<String> createBurnoutRiskActionItems() {
        return Arrays.asList(
                "Take regular breaks throughout the day",
                "Consider delegating or postponing non-critical tasks",
                "Evaluate current workload with supervisor or team",
                "Prioritize self-care and work-life balance",
                "Seek support if feeling overwhelmed"
        );
    }

    private List<String> identifyEfficiencyOpportunities(List<TaskDto> tasks, ProductivityForecast forecast) {
        List<String> opportunities = new ArrayList<>();
        
        // Analyze task patterns for opportunities
        Map<String, Long> statusCounts = tasks.stream()
                .collect(Collectors.groupingBy(TaskDto::getStatus, Collectors.counting()));
        
        long inProgressTasks = statusCounts.getOrDefault("IN_PROGRESS", 0L);
        long totalTasks = tasks.size();
        
        if (totalTasks > 0 && (double) inProgressTasks / totalTasks > 0.3) {
            opportunities.add("Consider focusing on completing existing tasks before starting new ones");
        }
        
        // Check for time estimation accuracy
        OptionalDouble avgEstimated = tasks.stream()
                .filter(task -> task.getEstimatedHours() != null && task.getActualHours() != null)
                .mapToDouble(task -> Math.abs(task.getEstimatedHours() - task.getActualHours()))
                .average();
        
        if (avgEstimated.isPresent() && avgEstimated.getAsDouble() > 2.0) {
            opportunities.add("Improve time estimation accuracy for better planning");
        }
        
        return opportunities;
    }

    private List<ProactiveAlert> prioritizeAndFilterAlerts(List<ProactiveAlert> alerts) {
        // Remove duplicate alert types for the same user
        Map<ProactiveAlert.AlertType, ProactiveAlert> uniqueAlerts = new HashMap<>();
        
        for (ProactiveAlert alert : alerts) {
            ProactiveAlert existing = uniqueAlerts.get(alert.getType());
            if (existing == null || alert.getSeverity().ordinal() > existing.getSeverity().ordinal()) {
                uniqueAlerts.put(alert.getType(), alert);
            }
        }
        
        // Sort by severity (highest first)
        return uniqueAlerts.values().stream()
                .sorted((a1, a2) -> Integer.compare(a2.getSeverity().ordinal(), a1.getSeverity().ordinal()))
                .collect(Collectors.toList());
    }

    private String generateAlertId() {
        return "ALERT_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static Map<ProactiveAlert.AlertType, AlertThreshold> createDefaultThresholds() {
        Map<ProactiveAlert.AlertType, AlertThreshold> thresholds = new HashMap<>();
        
        thresholds.put(ProactiveAlert.AlertType.PRODUCTIVITY_DROP, AlertThreshold.builder()
                .thresholdId("PROD_DROP_DEFAULT")
                .alertType(ProactiveAlert.AlertType.PRODUCTIVITY_DROP)
                .warningThreshold(4.0)
                .criticalThreshold(2.5)
                .minimumDataPoints(5)
                .confidenceThreshold(0.5)
                .enabled(true)
                .description("Productivity score below expected levels")
                .build());
        
        thresholds.put(ProactiveAlert.AlertType.TREND_DECLINE, AlertThreshold.builder()
                .thresholdId("TREND_DECLINE_DEFAULT")
                .alertType(ProactiveAlert.AlertType.TREND_DECLINE)
                .warningThreshold(0.3)
                .criticalThreshold(0.6)
                .minimumDataPoints(10)
                .confidenceThreshold(0.6)
                .enabled(true)
                .description("Declining productivity trend strength")
                .build());
        
        thresholds.put(ProactiveAlert.AlertType.PATTERN_ANOMALY, AlertThreshold.builder()
                .thresholdId("PATTERN_ANOMALY_DEFAULT")
                .alertType(ProactiveAlert.AlertType.PATTERN_ANOMALY)
                .warningThreshold(0.4)
                .criticalThreshold(0.7)
                .minimumDataPoints(7)
                .confidenceThreshold(0.5)
                .enabled(true)
                .description("Significant deviation from normal patterns")
                .build());
        
        thresholds.put(ProactiveAlert.AlertType.WORKLOAD_IMBALANCE, AlertThreshold.builder()
                .thresholdId("WORKLOAD_IMBALANCE_DEFAULT")
                .alertType(ProactiveAlert.AlertType.WORKLOAD_IMBALANCE)
                .warningThreshold(0.5)
                .criticalThreshold(0.8)
                .minimumDataPoints(10)
                .confidenceThreshold(0.7)
                .enabled(true)
                .description("Uneven distribution of task priorities or workload")
                .build());
        
        thresholds.put(ProactiveAlert.AlertType.BURNOUT_RISK, AlertThreshold.builder()
                .thresholdId("BURNOUT_RISK_DEFAULT")
                .alertType(ProactiveAlert.AlertType.BURNOUT_RISK)
                .warningThreshold(0.6)
                .criticalThreshold(0.8)
                .minimumDataPoints(15)
                .confidenceThreshold(0.7)
                .enabled(true)
                .description("Risk factors indicating potential burnout")
                .build());
        
        return thresholds;
    }
}