package com.taskmanagement.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductivityForecast {
    private String userId;
    private List<DailyProductivityPrediction> dailyPredictions;
    private double overallForecastScore;
    private double confidence;
    private String forecastMethod;
    private LocalDateTime generatedAt;
    private LocalDate forecastStartDate;
    private LocalDate forecastEndDate;
    private Map<String, Object> metadata;
    private List<String> assumptions;
    private double uncertaintyRange;
}