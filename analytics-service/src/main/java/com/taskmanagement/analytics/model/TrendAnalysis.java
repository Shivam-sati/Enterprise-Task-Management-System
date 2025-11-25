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
public class TrendAnalysis {
    private String userId;
    private List<CompletionTrend> completionTrends;
    private ProductivityPattern productivityPattern;
    private List<String> insights;
    private Map<String, Double> performanceMetrics;
    private LocalDateTime analyzedAt;
    private double confidence;
    private String trendDirection;
    private double trendStrength;
}