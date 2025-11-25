package com.taskmanagement.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertThreshold {
    private String thresholdId;
    private ProactiveAlert.AlertType alertType;
    private double warningThreshold;
    private double criticalThreshold;
    private int minimumDataPoints;
    private double confidenceThreshold;
    private boolean enabled;
    private String description;
    
    // Sensitivity levels
    public enum Sensitivity {
        LOW(0.3),
        MEDIUM(0.5),
        HIGH(0.7),
        VERY_HIGH(0.9);
        
        private final double multiplier;
        
        Sensitivity(double multiplier) {
            this.multiplier = multiplier;
        }
        
        public double getMultiplier() {
            return multiplier;
        }
    }
}