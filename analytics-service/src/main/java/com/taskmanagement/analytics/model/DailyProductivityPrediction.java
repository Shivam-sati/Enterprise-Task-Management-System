package com.taskmanagement.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyProductivityPrediction {
    private LocalDate date;
    private double predictedScore;
    private double confidence;
    private double lowerBound;
    private double upperBound;
    private int expectedTasksCompleted;
    private double expectedCompletionRate;
    private String dayOfWeekPattern;
    private String reasoning;
}