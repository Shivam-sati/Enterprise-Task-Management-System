package com.taskmanagement.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductivityPattern {
    private int bestHour;
    private DayOfWeek bestDay;
    private double averageSessionTime;
    private Map<DayOfWeek, Double> dailyProductivity;
    private Map<Integer, Double> hourlyProductivity;
    private String workingPattern;
    private double consistency;
}