package com.taskmanagement.analytics.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Test
    void testRecordRequest() {
        // Record a request
        metricsService.recordRequest("/analytics/productivity/user123", "GET");

        // Verify counter was incremented
        double count = meterRegistry.counter("analytics_requests_total",
                "endpoint", "/analytics/productivity/user123",
                "method", "GET").count();
        assertEquals(1.0, count);
    }

    @Test
    void testRequestTimer() {
        // Start and stop timer
        var sample = metricsService.startRequestTimer("/test");
        
        // Simulate some processing time
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metricsService.stopRequestTimer(sample, "/test");

        // Verify timer was recorded
        var timer = meterRegistry.timer("analytics_request_duration", "endpoint", "/test");
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) > 0);
    }

    @Test
    void testCalculationMetrics() {
        // Record calculation
        metricsService.recordCalculation("productivity");

        // Verify counter was incremented
        double count = meterRegistry.counter("analytics_calculations_total",
                "type", "productivity").count();
        assertEquals(1.0, count);
    }

    @Test
    void testCalculationTimer() {
        // Start and stop calculation timer
        var sample = metricsService.startCalculationTimer("trend_analysis");
        
        // Simulate calculation time
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metricsService.stopCalculationTimer(sample, "trend_analysis");

        // Verify timer was recorded
        var timer = meterRegistry.timer("analytics_calculation_duration", "type", "trend_analysis");
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) > 0);
    }

    @Test
    void testCalculationError() {
        // Record calculation error
        metricsService.recordCalculationError("productivity", "insufficient_data");

        // Verify error counter was incremented
        double count = meterRegistry.counter("analytics_calculation_errors_total",
                "type", "productivity",
                "error", "insufficient_data").count();
        assertEquals(1.0, count);
    }

    @Test
    void testTasksProcessed() {
        // Record tasks processed
        metricsService.recordTasksProcessed(25);

        // Verify gauge value
        Gauge tasksGauge = meterRegistry.find("analytics_total_tasks_processed").gauge();
        assertNotNull(tasksGauge);
        assertEquals(25.0, tasksGauge.value());

        // Record more tasks
        metricsService.recordTasksProcessed(15);
        Gauge updatedTasksGauge = meterRegistry.find("analytics_total_tasks_processed").gauge();
        assertNotNull(updatedTasksGauge);
        assertEquals(40.0, updatedTasksGauge.value()); // Should be cumulative
    }

    @Test
    void testCacheMetrics() {
        // Record cache operations
        metricsService.recordCacheHit();
        metricsService.recordCacheHit();
        metricsService.recordCacheMiss();

        // Verify cache hit ratio
        Gauge hitRatioGauge = meterRegistry.find("analytics_cache_hit_ratio").gauge();
        assertNotNull(hitRatioGauge);
        assertEquals(2.0/3.0, hitRatioGauge.value(), 0.001); // 2 hits out of 3 total operations
    }

    @Test
    void testPredictionMetrics() {
        // Record prediction
        metricsService.recordPrediction("productivity_forecast");

        // Verify counter was incremented
        double count = meterRegistry.counter("analytics_predictions_total",
                "type", "productivity_forecast").count();
        assertEquals(1.0, count);
    }

    @Test
    void testPredictionTimer() {
        // Start and stop prediction timer
        var sample = metricsService.startPredictionTimer("productivity_forecast");
        
        // Simulate prediction time
        try {
            Thread.sleep(8);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metricsService.stopPredictionTimer(sample, "productivity_forecast");

        // Verify timer was recorded
        var timer = meterRegistry.timer("analytics_prediction_duration", "type", "productivity_forecast");
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) > 0);
    }

    @Test
    void testAlertMetrics() {
        // Record alerts
        metricsService.recordAlert("productivity_drop", "high");
        metricsService.recordAlert("productivity_drop", "medium");
        metricsService.recordAlert("task_overdue", "low");

        // Verify alert counters
        double highAlerts = meterRegistry.counter("analytics_alerts_total",
                "type", "productivity_drop",
                "severity", "high").count();
        assertEquals(1.0, highAlerts);

        double mediumAlerts = meterRegistry.counter("analytics_alerts_total",
                "type", "productivity_drop",
                "severity", "medium").count();
        assertEquals(1.0, mediumAlerts);

        double lowAlerts = meterRegistry.counter("analytics_alerts_total",
                "type", "task_overdue",
                "severity", "low").count();
        assertEquals(1.0, lowAlerts);
    }

    @Test
    void testDatabaseQueryMetrics() {
        // Record database query
        Duration queryDuration = Duration.ofMillis(150);
        metricsService.recordDatabaseQuery("task_aggregation", queryDuration);

        // Verify timer was recorded
        var timer = meterRegistry.timer("analytics_database_query_duration", "query_type", "task_aggregation");
        assertEquals(1, timer.count());
        assertEquals(150.0, timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS), 1.0);
    }

    @Test
    void testServiceCallMetrics() {
        // Record successful service call
        Duration callDuration = Duration.ofMillis(200);
        metricsService.recordServiceCall("task-service", callDuration, true);

        // Record failed service call
        Duration failedCallDuration = Duration.ofMillis(5000);
        metricsService.recordServiceCall("task-service", failedCallDuration, false);

        // Verify timer was recorded for both calls
        var successTimer = meterRegistry.timer("analytics_service_call_duration",
                "service", "task-service", "success", "true");
        assertEquals(1, successTimer.count());
        assertEquals(200.0, successTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS), 1.0);

        var failureTimer = meterRegistry.timer("analytics_service_call_duration",
                "service", "task-service", "success", "false");
        assertEquals(1, failureTimer.count());
        assertEquals(5000.0, failureTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS), 1.0);

        // Verify counters
        double successCount = meterRegistry.counter("analytics_service_calls_total",
                "service", "task-service", "success", "true").count();
        assertEquals(1.0, successCount);

        double failureCount = meterRegistry.counter("analytics_service_calls_total",
                "service", "task-service", "success", "false").count();
        assertEquals(1.0, failureCount);
    }

    @Test
    void testActiveCalculationsGauge() {
        // Start multiple calculations
        var sample1 = metricsService.startCalculationTimer("type1");
        var sample2 = metricsService.startCalculationTimer("type2");

        // Verify active calculations count
        Gauge activeGauge = meterRegistry.find("analytics_active_calculations").gauge();
        assertNotNull(activeGauge);
        assertEquals(2.0, activeGauge.value());

        // Stop one calculation
        metricsService.stopCalculationTimer(sample1, "type1");
        Gauge activeGauge2 = meterRegistry.find("analytics_active_calculations").gauge();
        assertNotNull(activeGauge2);
        assertEquals(1.0, activeGauge2.value());

        // Stop remaining calculation
        metricsService.stopCalculationTimer(sample2, "type2");
        Gauge activeGauge3 = meterRegistry.find("analytics_active_calculations").gauge();
        assertNotNull(activeGauge3);
        assertEquals(0.0, activeGauge3.value());
    }

    @Test
    void testCacheHitRatioCalculation() {
        // Initially should be 0 (no operations)
        Gauge initialGauge = meterRegistry.find("analytics_cache_hit_ratio").gauge();
        assertNotNull(initialGauge);
        assertEquals(0.0, initialGauge.value());

        // Record only hits
        metricsService.recordCacheHit();
        metricsService.recordCacheHit();
        Gauge hitOnlyGauge = meterRegistry.find("analytics_cache_hit_ratio").gauge();
        assertNotNull(hitOnlyGauge);
        assertEquals(1.0, hitOnlyGauge.value());

        // Add some misses
        metricsService.recordCacheMiss();
        metricsService.recordCacheMiss();
        Gauge mixedGauge = meterRegistry.find("analytics_cache_hit_ratio").gauge();
        assertNotNull(mixedGauge);
        assertEquals(0.5, mixedGauge.value()); // 2 hits out of 4 total operations
    }
}