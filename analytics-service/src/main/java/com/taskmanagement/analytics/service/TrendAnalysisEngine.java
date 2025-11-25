package com.taskmanagement.analytics.service;

import com.taskmanagement.analytics.dto.TaskDto;
import com.taskmanagement.analytics.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendAnalysisEngine {

    private final TaskServiceClient taskServiceClient;
    
    private static final int MIN_TREND_DAYS = 7;
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.6;

    @Cacheable(value = "trendAnalysis", key = "#userId + '_' + #period.toString()")
    public TrendAnalysis analyzeTrends(String userId, Period period) {
        log.info("Analyzing trends for user: {} over period: {}", userId, period);
        
        try {
            List<TaskDto> tasks = taskServiceClient.getTasksForUser(userId, period);
            return buildTrendAnalysis(userId, tasks, period);
        } catch (Exception e) {
            log.error("Error analyzing trends for user: {}", userId, e);
            return createEmptyTrendAnalysis(userId);
        }
    }

    /**
     * Analyze trends using provided task data (for testing and when tasks are already available)
     */
    public TrendAnalysis analyzeTrends(String userId, List<TaskDto> tasks) {
        log.info("Analyzing trends for user: {} with {} tasks", userId, tasks.size());
        
        try {
            // Use a default 30-day period for analysis
            Period defaultPeriod = Period.ofDays(30);
            return buildTrendAnalysis(userId, tasks, defaultPeriod);
        } catch (Exception e) {
            log.error("Error analyzing trends for user: {}", userId, e);
            return createEmptyTrendAnalysis(userId);
        }
    }

    private TrendAnalysis buildTrendAnalysis(String userId, List<TaskDto> tasks, Period period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.minus(period);
        
        // Filter tasks within the period
        List<TaskDto> periodTasks = tasks.stream()
                .filter(task -> task.getCreatedAt().isAfter(periodStart))
                .collect(Collectors.toList());

        if (periodTasks.size() < MIN_TREND_DAYS) {
            return createInsufficientDataTrendAnalysis(userId);
        }

        // Calculate completion trends
        List<CompletionTrend> completionTrends = calculateCompletionTrends(periodTasks, periodStart, now);
        
        // Analyze productivity patterns
        ProductivityPattern productivityPattern = analyzeProductivityPatterns(periodTasks);
        
        // Generate insights
        List<String> insights = generateInsights(completionTrends, productivityPattern, periodTasks);
        
        // Calculate performance metrics
        Map<String, Double> performanceMetrics = calculatePerformanceMetrics(periodTasks, completionTrends);
        
        // Calculate trend direction and strength
        String trendDirection = calculateTrendDirection(completionTrends);
        double trendStrength = calculateTrendStrength(completionTrends);
        
        // Calculate confidence
        double confidence = calculateTrendConfidence(periodTasks.size(), completionTrends.size());

        return TrendAnalysis.builder()
                .userId(userId)
                .completionTrends(completionTrends)
                .productivityPattern(productivityPattern)
                .insights(insights)
                .performanceMetrics(performanceMetrics)
                .analyzedAt(now)
                .confidence(confidence)
                .trendDirection(trendDirection)
                .trendStrength(trendStrength)
                .build();
    }

    private List<CompletionTrend> calculateCompletionTrends(List<TaskDto> tasks, LocalDateTime periodStart, LocalDateTime periodEnd) {
        Map<LocalDate, CompletionTrend> trendMap = new HashMap<>();
        
        // Initialize all dates in the period
        LocalDate startDate = periodStart.toLocalDate();
        LocalDate endDate = periodEnd.toLocalDate();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            trendMap.put(date, CompletionTrend.builder()
                    .date(date)
                    .completed(0)
                    .created(0)
                    .completionRate(0.0)
                    .hoursSpent(0.0)
                    .productivity(0.0)
                    .build());
        }
        
        // Count created tasks by date
        tasks.stream()
                .filter(task -> task.getCreatedAt() != null)
                .forEach(task -> {
                    LocalDate createdDate = task.getCreatedAt().toLocalDate();
                    if (trendMap.containsKey(createdDate)) {
                        CompletionTrend trend = trendMap.get(createdDate);
                        trend.setCreated(trend.getCreated() + 1);
                    }
                });
        
        // Count completed tasks by date
        tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()) && task.getCompletedAt() != null)
                .forEach(task -> {
                    LocalDate completedDate = task.getCompletedAt().toLocalDate();
                    if (trendMap.containsKey(completedDate)) {
                        CompletionTrend trend = trendMap.get(completedDate);
                        trend.setCompleted(trend.getCompleted() + 1);
                        
                        // Add hours spent
                        if (task.getActualHours() != null) {
                            trend.setHoursSpent(trend.getHoursSpent() + task.getActualHours());
                        }
                    }
                });
        
        // Calculate completion rates and productivity
        trendMap.values().forEach(trend -> {
            if (trend.getCreated() > 0) {
                trend.setCompletionRate((double) trend.getCompleted() / trend.getCreated() * 100);
            }
            
            // Calculate productivity (tasks completed per hour)
            if (trend.getHoursSpent() > 0) {
                trend.setProductivity((double) trend.getCompleted() / trend.getHoursSpent());
            } else if (trend.getCompleted() > 0) {
                trend.setProductivity(trend.getCompleted()); // Assume 1 hour per task if no time data
            }
        });
        
        return trendMap.values().stream()
                .sorted(Comparator.comparing(CompletionTrend::getDate))
                .collect(Collectors.toList());
    }

    private ProductivityPattern analyzeProductivityPatterns(List<TaskDto> tasks) {
        List<TaskDto> completedTasks = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()) && task.getCompletedAt() != null)
                .collect(Collectors.toList());

        if (completedTasks.isEmpty()) {
            return createEmptyProductivityPattern();
        }

        // Analyze by day of week
        Map<DayOfWeek, Long> completionsByDay = completedTasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getCompletedAt().getDayOfWeek(),
                        Collectors.counting()
                ));
        
        Map<DayOfWeek, Double> dailyProductivity = completionsByDay.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().doubleValue()
                ));
        
        DayOfWeek bestDay = completionsByDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DayOfWeek.MONDAY);

        // Analyze by hour of day
        Map<Integer, Long> completionsByHour = completedTasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getCompletedAt().getHour(),
                        Collectors.counting()
                ));
        
        Map<Integer, Double> hourlyProductivity = completionsByHour.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().doubleValue()
                ));
        
        int bestHour = completionsByHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(9);

        // Calculate average session time
        double averageSessionTime = calculateAverageSessionTime(completedTasks);
        
        // Determine working pattern
        String workingPattern = determineWorkingPattern(hourlyProductivity);
        
        // Calculate consistency
        double consistency = calculateConsistency(dailyProductivity.values());

        return ProductivityPattern.builder()
                .bestHour(bestHour)
                .bestDay(bestDay)
                .averageSessionTime(averageSessionTime)
                .dailyProductivity(dailyProductivity)
                .hourlyProductivity(hourlyProductivity)
                .workingPattern(workingPattern)
                .consistency(consistency)
                .build();
    }

    private double calculateAverageSessionTime(List<TaskDto> completedTasks) {
        List<Double> sessionTimes = completedTasks.stream()
                .filter(task -> task.getCreatedAt() != null && task.getCompletedAt() != null)
                .map(task -> (double) ChronoUnit.MINUTES.between(task.getCreatedAt(), task.getCompletedAt()))
                .filter(minutes -> minutes > 0 && minutes < 480) // Filter out unrealistic times (more than 8 hours)
                .collect(Collectors.toList());

        return sessionTimes.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(45.0); // Default 45 minutes
    }

    private String determineWorkingPattern(Map<Integer, Double> hourlyProductivity) {
        if (hourlyProductivity.isEmpty()) {
            return "UNKNOWN";
        }
        
        // Find peak hours
        List<Integer> peakHours = hourlyProductivity.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        if (peakHours.isEmpty()) {
            return "UNKNOWN";
        }
        
        int earliestPeak = Collections.min(peakHours);
        int latestPeak = Collections.max(peakHours);
        
        if (earliestPeak >= 6 && latestPeak <= 12) {
            return "MORNING_PERSON";
        } else if (earliestPeak >= 13 && latestPeak <= 18) {
            return "AFTERNOON_PERSON";
        } else if (earliestPeak >= 19 || latestPeak <= 2) {
            return "NIGHT_OWL";
        } else {
            return "FLEXIBLE";
        }
    }

    private double calculateConsistency(Collection<Double> values) {
        if (values.size() < 2) {
            return 0.5;
        }
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (mean == 0) {
            return 0.0;
        }
        
        double variance = values.stream()
                .mapToDouble(val -> Math.pow(val - mean, 2))
                .average()
                .orElse(0.0);
        
        double coefficientOfVariation = Math.sqrt(variance) / mean;
        return Math.max(0.0, 1.0 - coefficientOfVariation);
    }

    private List<String> generateInsights(List<CompletionTrend> trends, ProductivityPattern pattern, List<TaskDto> tasks) {
        List<String> insights = new ArrayList<>();
        
        // Trend insights
        if (trends.size() >= 7) {
            double recentAvg = trends.subList(trends.size() - 3, trends.size()).stream()
                    .mapToDouble(CompletionTrend::getCompletionRate)
                    .average()
                    .orElse(0.0);
            
            double earlierAvg = trends.subList(0, Math.min(3, trends.size())).stream()
                    .mapToDouble(CompletionTrend::getCompletionRate)
                    .average()
                    .orElse(0.0);
            
            if (recentAvg > earlierAvg + 10) {
                insights.add("Your productivity has been improving recently");
            } else if (recentAvg < earlierAvg - 10) {
                insights.add("Your productivity has declined recently - consider reviewing your workload");
            }
        }
        
        // Pattern insights
        if (pattern.getBestDay() != null) {
            insights.add(String.format("You're most productive on %ss", pattern.getBestDay().name()));
        }
        
        if (pattern.getBestHour() >= 6 && pattern.getBestHour() <= 22) {
            insights.add(String.format("Your peak productivity time is around %d:00", pattern.getBestHour()));
        }
        
        if (pattern.getConsistency() > 0.7) {
            insights.add("You maintain consistent productivity levels");
        } else if (pattern.getConsistency() < 0.4) {
            insights.add("Your productivity varies significantly - try establishing a routine");
        }
        
        // Task completion insights
        long overdueTasks = tasks.stream()
                .filter(task -> task.getDueDate() != null)
                .filter(task -> LocalDateTime.now().isAfter(task.getDueDate()))
                .filter(task -> !"COMPLETED".equals(task.getStatus()))
                .count();
        
        if (overdueTasks > 0) {
            insights.add(String.format("You have %d overdue tasks - consider prioritizing them", overdueTasks));
        }
        
        return insights;
    }

    private Map<String, Double> calculatePerformanceMetrics(List<TaskDto> tasks, List<CompletionTrend> trends) {
        Map<String, Double> metrics = new HashMap<>();
        
        // Overall completion rate
        long completed = tasks.stream().filter(task -> "COMPLETED".equals(task.getStatus())).count();
        double completionRate = tasks.isEmpty() ? 0.0 : (double) completed / tasks.size() * 100;
        metrics.put("overallCompletionRate", completionRate);
        
        // Average daily completions
        double avgDailyCompletions = trends.stream()
                .mapToDouble(CompletionTrend::getCompleted)
                .average()
                .orElse(0.0);
        metrics.put("averageDailyCompletions", avgDailyCompletions);
        
        // Productivity trend (slope of completion rates)
        if (trends.size() >= 3) {
            double productivityTrend = calculateLinearTrend(trends);
            metrics.put("productivityTrend", productivityTrend);
        }
        
        // Task velocity (tasks per day)
        long daysCovered = trends.stream().filter(t -> t.getCompleted() > 0 || t.getCreated() > 0).count();
        double velocity = daysCovered > 0 ? (double) completed / daysCovered : 0.0;
        metrics.put("taskVelocity", velocity);
        
        return metrics;
    }

    private double calculateLinearTrend(List<CompletionTrend> trends) {
        int n = trends.size();
        if (n < 2) return 0.0;
        
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = trends.get(i).getCompletionRate();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) return 0.0;
        
        return (n * sumXY - sumX * sumY) / denominator;
    }

    private String calculateTrendDirection(List<CompletionTrend> trends) {
        if (trends.size() < 3) {
            return "INSUFFICIENT_DATA";
        }
        
        double slope = calculateLinearTrend(trends);
        
        if (slope > 1.0) {
            return "IMPROVING";
        } else if (slope < -1.0) {
            return "DECLINING";
        } else {
            return "STABLE";
        }
    }

    private double calculateTrendStrength(List<CompletionTrend> trends) {
        if (trends.size() < 3) {
            return 0.0;
        }
        
        double slope = Math.abs(calculateLinearTrend(trends));
        return Math.min(1.0, slope / 10.0); // Normalize to 0-1 scale
    }

    private double calculateTrendConfidence(int taskCount, int trendDays) {
        if (taskCount < 10 || trendDays < MIN_TREND_DAYS) {
            return 0.3;
        } else if (taskCount >= 50 && trendDays >= 14) {
            return HIGH_CONFIDENCE_THRESHOLD;
        } else if (taskCount >= 25 && trendDays >= 10) {
            return MEDIUM_CONFIDENCE_THRESHOLD;
        } else {
            return 0.5;
        }
    }

    private ProductivityPattern createEmptyProductivityPattern() {
        return ProductivityPattern.builder()
                .bestHour(9)
                .bestDay(DayOfWeek.MONDAY)
                .averageSessionTime(45.0)
                .dailyProductivity(new HashMap<>())
                .hourlyProductivity(new HashMap<>())
                .workingPattern("UNKNOWN")
                .consistency(0.0)
                .build();
    }

    private TrendAnalysis createEmptyTrendAnalysis(String userId) {
        return TrendAnalysis.builder()
                .userId(userId)
                .completionTrends(new ArrayList<>())
                .productivityPattern(createEmptyProductivityPattern())
                .insights(List.of("No data available for trend analysis"))
                .performanceMetrics(new HashMap<>())
                .analyzedAt(LocalDateTime.now())
                .confidence(0.0)
                .trendDirection("NO_DATA")
                .trendStrength(0.0)
                .build();
    }

    private TrendAnalysis createInsufficientDataTrendAnalysis(String userId) {
        return TrendAnalysis.builder()
                .userId(userId)
                .completionTrends(new ArrayList<>())
                .productivityPattern(createEmptyProductivityPattern())
                .insights(List.of("Insufficient data for reliable trend analysis. Complete more tasks to see patterns."))
                .performanceMetrics(new HashMap<>())
                .analyzedAt(LocalDateTime.now())
                .confidence(0.2)
                .trendDirection("INSUFFICIENT_DATA")
                .trendStrength(0.0)
                .build();
    }
}