package com.taskmanagement.analytics.service;

import com.taskmanagement.analytics.dto.TaskDto;
import com.taskmanagement.analytics.model.DailyTaskCount;
import com.taskmanagement.analytics.model.ProductivityMetrics;
import com.taskmanagement.analytics.model.TaskStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsCalculator {

    private static final int MIN_DATA_POINTS = 5;
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.5;

    public ProductivityMetrics calculateProductivity(String userId, List<TaskDto> tasks, Period period) {
        log.info("Calculating productivity metrics for user: {} with {} tasks", userId, tasks.size());
        
        if (tasks.isEmpty()) {
            return createEmptyProductivityMetrics(userId, period);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.minus(period);
        
        // Filter tasks within the period
        List<TaskDto> periodTasks = tasks.stream()
                .filter(task -> task.getCreatedAt().isAfter(periodStart))
                .collect(Collectors.toList());

        if (periodTasks.isEmpty()) {
            return createEmptyProductivityMetrics(userId, period);
        }

        // Calculate basic metrics
        int totalTasks = periodTasks.size();
        int completedTasks = (int) periodTasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .count();
        
        double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0.0;
        
        // Calculate average task time
        double averageTaskTime = calculateAverageTaskTime(periodTasks);
        
        // Calculate productivity score (weighted combination of metrics)
        double productivityScore = calculateProductivityScore(completionRate, averageTaskTime, periodTasks);
        
        // Calculate confidence and data quality
        double confidence = calculateConfidence(totalTasks, completedTasks);
        String dataQuality = determineDataQuality(totalTasks, completedTasks);
        
        // Create breakdown
        Map<String, Object> breakdown = createProductivityBreakdown(periodTasks);

        return ProductivityMetrics.builder()
                .userId(userId)
                .completionRate(completionRate)
                .averageTaskTime(averageTaskTime)
                .tasksCompleted(completedTasks)
                .tasksCreated(totalTasks)
                .productivityScore(productivityScore)
                .period(period)
                .calculatedAt(now)
                .breakdown(breakdown)
                .confidence(confidence)
                .dataQuality(dataQuality)
                .build();
    }

    private double calculateAverageTaskTime(List<TaskDto> tasks) {
        List<Double> completionTimes = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .filter(task -> task.getCompletedAt() != null && task.getCreatedAt() != null)
                .map(task -> (double) ChronoUnit.HOURS.between(task.getCreatedAt(), task.getCompletedAt()))
                .filter(hours -> hours > 0 && hours < 168) // Filter out unrealistic times (more than a week)
                .collect(Collectors.toList());

        if (completionTimes.isEmpty()) {
            // Fallback to estimated hours if available
            OptionalDouble avgEstimated = tasks.stream()
                    .filter(task -> task.getEstimatedHours() != null && task.getEstimatedHours() > 0)
                    .mapToDouble(TaskDto::getEstimatedHours)
                    .average();
            return avgEstimated.orElse(2.0); // Default estimate
        }

        return completionTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateProductivityScore(double completionRate, double averageTaskTime, List<TaskDto> tasks) {
        // Base score from completion rate (0-4 points)
        double completionScore = (completionRate / 100.0) * 4.0;
        
        // Time efficiency score (0-3 points) - lower average time is better
        double timeScore = Math.max(0, 3.0 - (averageTaskTime / 8.0)); // 8 hours as baseline
        
        // Priority completion bonus (0-2 points)
        double priorityScore = calculatePriorityScore(tasks);
        
        // Consistency bonus (0-1 point)
        double consistencyScore = calculateConsistencyScore(tasks);
        
        double totalScore = completionScore + timeScore + priorityScore + consistencyScore;
        return Math.min(10.0, Math.max(0.0, totalScore)); // Clamp between 0-10
    }

    private double calculatePriorityScore(List<TaskDto> tasks) {
        long highPriorityCompleted = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .filter(task -> "HIGH".equals(task.getPriority()) || "CRITICAL".equals(task.getPriority()))
                .count();
        
        long totalHighPriority = tasks.stream()
                .filter(task -> "HIGH".equals(task.getPriority()) || "CRITICAL".equals(task.getPriority()))
                .count();
        
        if (totalHighPriority == 0) return 1.0; // Neutral score if no high priority tasks
        
        return (double) highPriorityCompleted / totalHighPriority * 2.0;
    }

    private double calculateConsistencyScore(List<TaskDto> tasks) {
        Map<LocalDate, Long> dailyCompletions = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .filter(task -> task.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        task -> task.getCompletedAt().toLocalDate(),
                        Collectors.counting()
                ));
        
        if (dailyCompletions.size() < 3) return 0.5; // Not enough data
        
        double variance = calculateVariance(dailyCompletions.values());
        double mean = dailyCompletions.values().stream().mapToDouble(Long::doubleValue).average().orElse(0.0);
        
        if (mean == 0) return 0.0;
        
        double coefficientOfVariation = Math.sqrt(variance) / mean;
        return Math.max(0.0, 1.0 - coefficientOfVariation); // Lower variation = higher consistency
    }

    private double calculateVariance(Collection<Long> values) {
        double mean = values.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);
        return values.stream()
                .mapToDouble(val -> Math.pow(val - mean, 2))
                .average()
                .orElse(0.0);
    }

    private double calculateConfidence(int totalTasks, int completedTasks) {
        if (totalTasks < MIN_DATA_POINTS) {
            return 0.3; // Low confidence with insufficient data
        }
        
        if (totalTasks >= 30 && completedTasks >= 10) {
            return HIGH_CONFIDENCE_THRESHOLD;
        } else if (totalTasks >= 15 && completedTasks >= 5) {
            return MEDIUM_CONFIDENCE_THRESHOLD;
        } else {
            return 0.4;
        }
    }

    private String determineDataQuality(int totalTasks, int completedTasks) {
        if (totalTasks < MIN_DATA_POINTS) {
            return "INSUFFICIENT_DATA";
        } else if (totalTasks >= 30 && completedTasks >= 10) {
            return "HIGH_QUALITY";
        } else if (totalTasks >= 15 && completedTasks >= 5) {
            return "MEDIUM_QUALITY";
        } else {
            return "LOW_QUALITY";
        }
    }

    private Map<String, Object> createProductivityBreakdown(List<TaskDto> tasks) {
        Map<String, Object> breakdown = new HashMap<>();
        
        // Status breakdown
        Map<String, Long> statusCounts = tasks.stream()
                .collect(Collectors.groupingBy(TaskDto::getStatus, Collectors.counting()));
        breakdown.put("statusBreakdown", statusCounts);
        
        // Priority breakdown
        Map<String, Long> priorityCounts = tasks.stream()
                .collect(Collectors.groupingBy(TaskDto::getPriority, Collectors.counting()));
        breakdown.put("priorityBreakdown", priorityCounts);
        
        // Time analysis
        Map<String, Object> timeAnalysis = new HashMap<>();
        OptionalDouble avgEstimated = tasks.stream()
                .filter(task -> task.getEstimatedHours() != null)
                .mapToDouble(TaskDto::getEstimatedHours)
                .average();
        OptionalDouble avgActual = tasks.stream()
                .filter(task -> task.getActualHours() != null)
                .mapToDouble(TaskDto::getActualHours)
                .average();
        
        timeAnalysis.put("averageEstimatedHours", avgEstimated.orElse(0.0));
        timeAnalysis.put("averageActualHours", avgActual.orElse(0.0));
        breakdown.put("timeAnalysis", timeAnalysis);
        
        return breakdown;
    }

    private ProductivityMetrics createEmptyProductivityMetrics(String userId, Period period) {
        return ProductivityMetrics.builder()
                .userId(userId)
                .completionRate(0.0)
                .averageTaskTime(0.0)
                .tasksCompleted(0)
                .tasksCreated(0)
                .productivityScore(0.0)
                .period(period)
                .calculatedAt(LocalDateTime.now())
                .breakdown(new HashMap<>())
                .confidence(0.0)
                .dataQuality("NO_DATA")
                .build();
    }
}