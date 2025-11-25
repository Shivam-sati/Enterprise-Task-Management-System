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
public class ProactiveAlert {
    private String alertId;
    private String userId;
    private AlertType type;
    private AlertSeverity severity;
    private String title;
    private String message;
    private String recommendation;
    private double confidence;
    private LocalDateTime triggeredAt;
    private LocalDateTime expiresAt;
    private boolean acknowledged;
    private Map<String, Object> context;
    private List<String> actionItems;
    private String triggerReason;
    
    public enum AlertType {
        PRODUCTIVITY_DROP,
        TREND_DECLINE,
        PATTERN_ANOMALY,
        WORKLOAD_IMBALANCE,
        BURNOUT_RISK,
        EFFICIENCY_OPPORTUNITY
    }
    
    public enum AlertSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}