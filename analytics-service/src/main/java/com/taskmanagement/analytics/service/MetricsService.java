package com.taskmanagement.analytics.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting and exposing custom metrics for Analytics service
 */
@Service
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;
    
    // Request metrics
    private final Counter requestCounter;
    private final Timer requestTimer;
    
    // Analytics calculation metrics
    private final Counter calculationCounter;
    private final Timer calculationTimer;
    private final Counter calculationErrorCounter;
    
    // Data metrics
    private final AtomicInteger activeCalculations = new AtomicInteger(0);
    private final AtomicLong totalTasksProcessed = new AtomicLong(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    
    // Prediction metrics
    private final Counter predictionCounter;
    private final Timer predictionTimer;
    private final Gauge predictionAccuracy;
    
    // Alert metrics
    private final Counter alertCounter;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters and timers
        this.requestCounter = Counter.builder("analytics_requests_total")
                .description("Total number of requests to analytics service")
                .tag("service", "analytics")
                .register(meterRegistry);
        
        this.requestTimer = Timer.builder("analytics_request_duration")
                .description("Request processing duration")
                .tag("service", "analytics")
                .register(meterRegistry);
        
        this.calculationCounter = Counter.builder("analytics_calculations_total")
                .description("Total number of analytics calculations")
                .tag("service", "analytics")
                .register(meterRegistry);
        
        this.calculationTimer = Timer.builder("analytics_calculation_duration")
                .description("Analytics calculation duration")
                .tag("service", "analytics")
                .register(meterRegistry);
        
        this.calculationErrorCounter = Counter.builder("analytics_calculation_errors_total")
                .description("Total number of calculation errors")
                .tag("service", "analytics")
                .register(meterRegistry);
        
        this.predictionCounter = Counter.builder("analytics_predictions_total")
                .description("Total number of predictions made")
                .tag("service", "analytics")
                .register(meterRegistry);
        
        this.predictionTimer = Timer.builder("analytics_prediction_duration")
                .description("Prediction processing duration")
                .tag("service", "analytics")
                .register(meterRegistry);
        
        this.alertCounter = Counter.builder("analytics_alerts_total")
                .description("Total number of alerts generated")
                .tag("service", "analytics")
                .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("analytics_active_calculations", this, MetricsService::getActiveCalculations)
                .description("Number of currently active calculations")
                .tag("service", "analytics")
                .register(meterRegistry);
        
        Gauge.builder("analytics_total_tasks_processed", this, MetricsService::getTotalTasksProcessed)
                .description("Total number of tasks processed")
                .tag("service", "analytics")
                .register(meterRegistry);
        
        Gauge.builder("analytics_cache_hit_ratio", this, MetricsService::getCacheHitRatio)
                .description("Cache hit ratio")
                .tag("service", "analytics")
                .register(meterRegistry);
        
        this.predictionAccuracy = Gauge.builder("analytics_prediction_accuracy", this, MetricsService::getPredictionAccuracy)
                .description("Current prediction accuracy score")
                .tag("service", "analytics")
                .register(meterRegistry);
        
        log.info("MetricsService initialized with custom metrics");
    }
    
    // Request metrics
    public void recordRequest(String endpoint, String method) {
        Counter.builder("analytics_requests_total")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .register(meterRegistry)
                .increment();
    }
    
    public Timer.Sample startRequestTimer(String endpoint) {
        return Timer.start(meterRegistry);
    }
    
    public void stopRequestTimer(Timer.Sample sample, String endpoint) {
        sample.stop(Timer.builder("analytics_request_duration")
                .tag("endpoint", endpoint)
                .register(meterRegistry));
    }
    
    // Calculation metrics
    public void recordCalculation(String calculationType) {
        Counter.builder("analytics_calculations_total")
                .tag("type", calculationType)
                .register(meterRegistry)
                .increment();
    }
    
    public Timer.Sample startCalculationTimer(String calculationType) {
        activeCalculations.incrementAndGet();
        return Timer.start(meterRegistry);
    }
    
    public void stopCalculationTimer(Timer.Sample sample, String calculationType) {
        activeCalculations.decrementAndGet();
        sample.stop(Timer.builder("analytics_calculation_duration")
                .tag("type", calculationType)
                .register(meterRegistry));
    }
    
    public void recordCalculationError(String calculationType, String errorType) {
        Counter.builder("analytics_calculation_errors_total")
                .tag("type", calculationType)
                .tag("error", errorType)
                .register(meterRegistry)
                .increment();
    }
    
    // Data processing metrics
    public void recordTasksProcessed(int count) {
        totalTasksProcessed.addAndGet(count);
    }
    
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }
    
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }
    
    // Prediction metrics
    public void recordPrediction(String predictionType) {
        Counter.builder("analytics_predictions_total")
                .tag("type", predictionType)
                .register(meterRegistry)
                .increment();
    }
    
    public Timer.Sample startPredictionTimer(String predictionType) {
        return Timer.start(meterRegistry);
    }
    
    public void stopPredictionTimer(Timer.Sample sample, String predictionType) {
        sample.stop(Timer.builder("analytics_prediction_duration")
                .tag("type", predictionType)
                .register(meterRegistry));
    }
    
    // Alert metrics
    public void recordAlert(String alertType, String severity) {
        Counter.builder("analytics_alerts_total")
                .tag("type", alertType)
                .tag("severity", severity)
                .register(meterRegistry)
                .increment();
    }
    
    // Gauge value providers
    private double getActiveCalculations() {
        return activeCalculations.get();
    }
    
    private double getTotalTasksProcessed() {
        return totalTasksProcessed.get();
    }
    
    private double getCacheHitRatio() {
        int hits = cacheHits.get();
        int misses = cacheMisses.get();
        int total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }
    
    private double getPredictionAccuracy() {
        // This would be calculated based on actual prediction accuracy
        // For now, return a placeholder value
        return 0.85; // 85% accuracy placeholder
    }
    
    // Utility methods for monitoring
    public void recordDatabaseQuery(String queryType, Duration duration) {
        Timer.builder("analytics_database_query_duration")
                .description("Database query duration")
                .tag("query_type", queryType)
                .register(meterRegistry)
                .record(duration);
    }
    
    public void recordServiceCall(String serviceName, Duration duration, boolean success) {
        Timer.builder("analytics_service_call_duration")
                .description("External service call duration")
                .tag("service", serviceName)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(duration);
        
        Counter.builder("analytics_service_calls_total")
                .description("Total external service calls")
                .tag("service", serviceName)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }
}