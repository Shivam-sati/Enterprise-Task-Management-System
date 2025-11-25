package com.taskmanagement.analytics.service;

import com.taskmanagement.analytics.dto.TaskDto;
import com.taskmanagement.analytics.model.DailyTaskCount;
import com.taskmanagement.analytics.model.TaskStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataAggregator {

    private final RestTemplate restTemplate;
    private final TaskServiceClient taskServiceClient;

    @Cacheable(value = "taskStatistics", key = "#userId + '_' + #period.toString()")
    public TaskStatistics aggregateTaskData(String userId, Period period) {
        log.info("Aggregating task data for user: {} over period: {}", userId, period);
        
        try {
            List<TaskDto> tasks = taskServiceClient.getTasksForUser(userId, period);
            return buildTaskStatistics(userId, tasks, period);
        } catch (Exception e) {
            log.error("Error aggregating task data for user: {}", userId, e);
            return createEmptyTaskStatistics(userId, period);
        }
    }

    public Map<String, Double> analyzeTimePatterns(String userId, Period period) {
        log.info("Analyzing time patterns for user: {}", userId);
        
        try {
            List<TaskDto> tasks = taskServiceClient.getTasksForUser(userId, period);
            return calculateTimePatterns(tasks);
        } catch (Exception e) {
            log.error("Error analyzing time patterns for user: {}", userId, e);
            return new HashMap<>();
        }
    }

    public Map<String, Integer> analyzeCategoryDistribution(String userId, Period period) {
        log.info("Analyzing category distribution for user: {}", userId);
        
        try {
            List<TaskDto> tasks = taskServiceClient.getTasksForUser(userId, period);
            return calculateCategoryBreakdown(tasks);
        } catch (Exception e) {
            log.error("Error analyzing category distribution for user: {}", userId, e);
            return new HashMap<>();
        }
    }

    private TaskStatistics buildTaskStatistics(String userId, List<TaskDto> tasks, Period period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.minus(period);
        
        // Filter tasks within the period
        List<TaskDto> periodTasks = tasks.stream()
                .filter(task -> task.getCreatedAt().isAfter(periodStart))
                .collect(Collectors.toList());

        // Calculate basic counts
        int totalTasks = periodTasks.size();
        int completedTasks = (int) periodTasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .count();
        int pendingTasks = (int) periodTasks.stream()
                .filter(task -> "TODO".equals(task.getStatus()) || "IN_PROGRESS".equals(task.getStatus()))
                .count();
        int overdueTasks = (int) periodTasks.stream()
                .filter(this::isOverdue)
                .count();
        int cancelledTasks = (int) periodTasks.stream()
                .filter(task -> "CANCELLED".equals(task.getStatus()))
                .count();

        // Calculate time metrics
        double averageCompletionTime = calculateAverageCompletionTime(periodTasks);
        double totalTimeSpent = calculateTotalTimeSpent(periodTasks);

        // Calculate breakdowns
        Map<String, Integer> tasksByPriority = calculatePriorityBreakdown(periodTasks);
        Map<String, Integer> tasksByCategory = calculateCategoryBreakdown(periodTasks);
        List<DailyTaskCount> dailyCounts = calculateDailyCounts(periodTasks, periodStart, now);

        return TaskStatistics.builder()
                .userId(userId)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .pendingTasks(pendingTasks)
                .overdueTasks(overdueTasks)
                .cancelledTasks(cancelledTasks)
                .averageCompletionTime(averageCompletionTime)
                .totalTimeSpent(totalTimeSpent)
                .tasksByPriority(tasksByPriority)
                .tasksByCategory(tasksByCategory)
                .dailyCounts(dailyCounts)
                .periodStart(periodStart)
                .periodEnd(now)
                .calculatedAt(now)
                .build();
    }

    private boolean isOverdue(TaskDto task) {
        return task.getDueDate() != null 
                && LocalDateTime.now().isAfter(task.getDueDate()) 
                && !"COMPLETED".equals(task.getStatus())
                && !"CANCELLED".equals(task.getStatus());
    }

    private double calculateAverageCompletionTime(List<TaskDto> tasks) {
        List<Double> completionTimes = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .filter(task -> task.getCompletedAt() != null && task.getCreatedAt() != null)
                .map(task -> (double) ChronoUnit.HOURS.between(task.getCreatedAt(), task.getCompletedAt()))
                .filter(hours -> hours > 0 && hours < 720) // Filter out unrealistic times (more than 30 days)
                .collect(Collectors.toList());

        return completionTimes.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double calculateTotalTimeSpent(List<TaskDto> tasks) {
        return tasks.stream()
                .filter(task -> task.getActualHours() != null)
                .mapToDouble(TaskDto::getActualHours)
                .sum();
    }

    private Map<String, Integer> calculatePriorityBreakdown(List<TaskDto> tasks) {
        return tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getPriority() != null ? task.getPriority() : "UNKNOWN",
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
    }

    private Map<String, Integer> calculateCategoryBreakdown(List<TaskDto> tasks) {
        Map<String, Integer> categoryCount = new HashMap<>();
        
        for (TaskDto task : tasks) {
            if (task.getTags() != null && !task.getTags().isEmpty()) {
                for (String tag : task.getTags()) {
                    categoryCount.merge(tag, 1, Integer::sum);
                }
            } else {
                categoryCount.merge("Uncategorized", 1, Integer::sum);
            }
        }
        
        return categoryCount;
    }

    private List<DailyTaskCount> calculateDailyCounts(List<TaskDto> tasks, LocalDateTime periodStart, LocalDateTime periodEnd) {
        Map<LocalDate, DailyTaskCount> dailyCountMap = new HashMap<>();
        
        // Initialize all dates in the period
        LocalDate startDate = periodStart.toLocalDate();
        LocalDate endDate = periodEnd.toLocalDate();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyCountMap.put(date, DailyTaskCount.builder()
                    .date(date)
                    .created(0)
                    .completed(0)
                    .cancelled(0)
                    .hoursSpent(0.0)
                    .build());
        }
        
        // Count created tasks
        tasks.stream()
                .filter(task -> task.getCreatedAt() != null)
                .forEach(task -> {
                    LocalDate createdDate = task.getCreatedAt().toLocalDate();
                    if (dailyCountMap.containsKey(createdDate)) {
                        DailyTaskCount count = dailyCountMap.get(createdDate);
                        count.setCreated(count.getCreated() + 1);
                    }
                });
        
        // Count completed tasks
        tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()) && task.getCompletedAt() != null)
                .forEach(task -> {
                    LocalDate completedDate = task.getCompletedAt().toLocalDate();
                    if (dailyCountMap.containsKey(completedDate)) {
                        DailyTaskCount count = dailyCountMap.get(completedDate);
                        count.setCompleted(count.getCompleted() + 1);
                        
                        // Add actual hours if available
                        if (task.getActualHours() != null) {
                            count.setHoursSpent(count.getHoursSpent() + task.getActualHours());
                        }
                    }
                });
        
        // Count cancelled tasks
        tasks.stream()
                .filter(task -> "CANCELLED".equals(task.getStatus()) && task.getUpdatedAt() != null)
                .forEach(task -> {
                    LocalDate cancelledDate = task.getUpdatedAt().toLocalDate();
                    if (dailyCountMap.containsKey(cancelledDate)) {
                        DailyTaskCount count = dailyCountMap.get(cancelledDate);
                        count.setCancelled(count.getCancelled() + 1);
                    }
                });
        
        return dailyCountMap.values().stream()
                .sorted(Comparator.comparing(DailyTaskCount::getDate))
                .collect(Collectors.toList());
    }

    private Map<String, Double> calculateTimePatterns(List<TaskDto> tasks) {
        Map<String, Double> patterns = new HashMap<>();
        
        // Calculate average completion time by priority
        Map<String, Double> avgTimeByPriority = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .filter(task -> task.getCompletedAt() != null && task.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        task -> task.getPriority() != null ? task.getPriority() : "UNKNOWN",
                        Collectors.averagingDouble(task -> 
                                ChronoUnit.HOURS.between(task.getCreatedAt(), task.getCompletedAt()))
                ));
        
        patterns.putAll(avgTimeByPriority.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> "avgTime_" + entry.getKey(),
                        Map.Entry::getValue
                )));
        
        // Calculate completion rate by day of week
        Map<Integer, Long> completionsByDayOfWeek = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()) && task.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        task -> task.getCompletedAt().getDayOfWeek().getValue(),
                        Collectors.counting()
                ));
        
        if (!completionsByDayOfWeek.isEmpty()) {
            int bestDay = completionsByDayOfWeek.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(1);
            patterns.put("bestDayOfWeek", (double) bestDay);
        }
        
        // Calculate best hour of day for completions
        Map<Integer, Long> completionsByHour = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()) && task.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        task -> task.getCompletedAt().getHour(),
                        Collectors.counting()
                ));
        
        if (!completionsByHour.isEmpty()) {
            int bestHour = completionsByHour.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(9);
            patterns.put("bestHour", (double) bestHour);
        }
        
        return patterns;
    }

    private TaskStatistics createEmptyTaskStatistics(String userId, Period period) {
        LocalDateTime now = LocalDateTime.now();
        return TaskStatistics.builder()
                .userId(userId)
                .totalTasks(0)
                .completedTasks(0)
                .pendingTasks(0)
                .overdueTasks(0)
                .cancelledTasks(0)
                .averageCompletionTime(0.0)
                .totalTimeSpent(0.0)
                .tasksByPriority(new HashMap<>())
                .tasksByCategory(new HashMap<>())
                .dailyCounts(new ArrayList<>())
                .periodStart(now.minus(period))
                .periodEnd(now)
                .calculatedAt(now)
                .build();
    }
}