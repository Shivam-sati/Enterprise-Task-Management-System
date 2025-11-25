package com.taskmanagement.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatistics {
    private String userId;
    private int totalTasks;
    private int completedTasks;
    private int pendingTasks;
    private int overdueTasks;
    private int cancelledTasks;
    private double averageCompletionTime;
    private double totalTimeSpent;
    private Map<String, Integer> tasksByPriority;
    private Map<String, Integer> tasksByCategory;
    private List<DailyTaskCount> dailyCounts;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime calculatedAt;
}