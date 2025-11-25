package com.taskmanagement.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductivityMetrics {
    private String userId;
    private double completionRate;
    private double averageTaskTime;
    private int tasksCompleted;
    private int tasksCreated;
    private double productivityScore;
    private Period period;
    private LocalDateTime calculatedAt;
    private Map<String, Object> breakdown;
    private double confidence;
    private String dataQuality;
}