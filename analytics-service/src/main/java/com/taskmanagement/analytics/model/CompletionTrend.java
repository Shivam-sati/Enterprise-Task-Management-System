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
public class CompletionTrend {
    private LocalDate date;
    private int completed;
    private int created;
    private double completionRate;
    private double hoursSpent;
    private double productivity;
}