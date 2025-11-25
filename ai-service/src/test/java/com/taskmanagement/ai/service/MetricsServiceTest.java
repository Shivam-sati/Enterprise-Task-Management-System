package com.taskmanagement.ai.service;

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
        metricsService.recordRequest("/ai/parse", "POST");

        // Verify counter was incremented
        double count = meterRegistry.counter("ai_service_requests_total",
                "endpoint", "/ai/parse",
                "method", "POST").count();
        assertEquals(1.0, count);
    }

    @Test
    void testRequestTimer() {
        // Start and stop timer
        var sample = metricsService.startRequestTimer("/ai/prioritize");
        
        // Simulate some processing time
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metricsService.stopRequestTimer(sample, "/ai/prioritize");

        // Verify timer was recorded
        var timer = meterRegistry.timer("ai_service_request_duration", "endpoint", "/ai/prioritize");
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) > 0);
    }

    @Test
    void testAIProcessingMetrics() {
        // Record AI request
        metricsService.recordAIRequest("task_parsing");

        // Verify counter was incremented
        double count = meterRegistry.counter("ai_processing_requests_total",
                "type", "task_parsing").count();
        assertEquals(1.0, count);
    }

    @Test
    void testAIProcessingTimer() {
        // Start and stop AI processing timer
        var sample = metricsService.startAIProcessingTimer("task_prioritization");
        
        // Simulate processing time
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metricsService.stopAIProcessingTimer(sample, "task_prioritization");

        // Verify timer was recorded
        var timer = meterRegistry.timer("ai_processing_duration", "type", "task_prioritization");
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) > 0);
    }

    @Test
    void testAIError() {
        // Record AI error
        metricsService.recordAIError("insights_generation", "model_timeout");

        // Verify error counter was incremented
        double count = meterRegistry.counter("ai_processing_errors_total",
                "type", "insights_generation",
                "error", "model_timeout").count();
        assertEquals(1.0, count);
    }

    @Test
    void testPythonServiceCallMetrics() {
        // Record Python service call
        metricsService.recordPythonServiceCall("/ai/parse");

        // Verify counter was incremented
        double count = meterRegistry.counter("python_service_calls_total",
                "endpoint", "/ai/parse").count();
        assertEquals(1.0, count);
    }

    @Test
    void testPythonServiceCallTimer() {
        // Start and stop Python service call timer
        var sample = metricsService.startPythonServiceCallTimer("/ai/insights");
        
        // Simulate call time
        try {
            Thread.sleep(8);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metricsService.stopPythonServiceCallTimer(sample, "/ai/insights", true);

        // Verify timer was recorded
        var timer = meterRegistry.timer("python_service_call_duration",
                "endpoint", "/ai/insights", "success", "true");
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) > 0);
    }

    @Test
    void testPythonServiceError() {
        // Record Python service error
        metricsService.recordPythonServiceError("/ai/parse", "connection_timeout");

        // Verify error counter was incremented
        double count = meterRegistry.counter("python_service_errors_total",
                "endpoint", "/ai/parse",
                "error", "connection_timeout").count();
        assertEquals(1.0, count);
    }

    @Test
    void testFallbackUsage() {
        // Record fallback usage
        metricsService.recordFallbackUsage("task_parsing", "python_service_unavailable");
        metricsService.recordFallbackUsage("task_prioritization", "circuit_breaker_open");

        // Verify fallback counters
        double parseCount = meterRegistry.counter("ai_fallback_responses_total",
                "type", "task_parsing",
                "reason", "python_service_unavailable").count();
        assertEquals(1.0, parseCount);

        double prioritizeCount = meterRegistry.counter("ai_fallback_responses_total",
                "type", "task_prioritization",
                "reason", "circuit_breaker_open").count();
        assertEquals(1.0, prioritizeCount);
    }

    @Test
    void testCircuitBreakerState() {
        // Test different circuit breaker states
        metricsService.updateCircuitBreakerState("closed");
        Gauge closedGauge = meterRegistry.find("ai_circuit_breaker_state").gauge();
        assertNotNull(closedGauge);
        assertEquals(0.0, closedGauge.value());

        metricsService.updateCircuitBreakerState("open");
        Gauge openGauge = meterRegistry.find("ai_circuit_breaker_state").gauge();
        assertNotNull(openGauge);
        assertEquals(1.0, openGauge.value());

        metricsService.updateCircuitBreakerState("half_open");
        Gauge halfOpenGauge = meterRegistry.find("ai_circuit_breaker_state").gauge();
        assertNotNull(halfOpenGauge);
        assertEquals(2.0, halfOpenGauge.value());

        metricsService.updateCircuitBreakerState("half-open");
        Gauge halfOpenGauge2 = meterRegistry.find("ai_circuit_breaker_state").gauge();
        assertNotNull(halfOpenGauge2);
        assertEquals(2.0, halfOpenGauge2.value());
    }

    @Test
    void testPythonServiceSuccessRate() {
        // Initially should be 0 (no calls)
        Gauge initialGauge = meterRegistry.find("ai_python_service_success_rate").gauge();
        assertNotNull(initialGauge);
        assertEquals(0.0, initialGauge.value());

        // Record successful calls
        var sample1 = metricsService.startPythonServiceCallTimer("/ai/parse");
        metricsService.recordPythonServiceCall("/ai/parse");
        metricsService.stopPythonServiceCallTimer(sample1, "/ai/parse", true);

        var sample2 = metricsService.startPythonServiceCallTimer("/ai/prioritize");
        metricsService.recordPythonServiceCall("/ai/prioritize");
        metricsService.stopPythonServiceCallTimer(sample2, "/ai/prioritize", true);

        // Should be 100% success rate
        Gauge successGauge = meterRegistry.find("ai_python_service_success_rate").gauge();
        assertNotNull(successGauge);
        assertEquals(1.0, successGauge.value());

        // Add a failed call
        var sample3 = metricsService.startPythonServiceCallTimer("/ai/insights");
        metricsService.recordPythonServiceCall("/ai/insights");
        metricsService.stopPythonServiceCallTimer(sample3, "/ai/insights", false);

        // Should be 66.7% success rate (2 out of 3)
        Gauge gauge = meterRegistry.find("ai_python_service_success_rate").gauge();
        assertNotNull(gauge);
        double mixedRate = gauge.value();
        assertEquals(2.0/3.0, mixedRate, 0.001);
    }

    @Test
    void testCacheOperation() {
        // Record cache operations
        metricsService.recordCacheOperation("get", true);
        metricsService.recordCacheOperation("get", false);
        metricsService.recordCacheOperation("set", true);

        // Verify cache operation counters
        double getHit = meterRegistry.counter("ai_cache_operations_total",
                "operation", "get", "hit", "true").count();
        assertEquals(1.0, getHit);

        double getMiss = meterRegistry.counter("ai_cache_operations_total",
                "operation", "get", "hit", "false").count();
        assertEquals(1.0, getMiss);

        double setOperation = meterRegistry.counter("ai_cache_operations_total",
                "operation", "set", "hit", "true").count();
        assertEquals(1.0, setOperation);
    }

    @Test
    void testModelOperation() {
        // Record model operation
        Duration operationDuration = Duration.ofMillis(250);
        metricsService.recordModelOperation("task_parser", "inference", operationDuration);

        // Verify timer was recorded
        var timer = meterRegistry.timer("ai_model_operation_duration",
                "model_type", "task_parser", "operation", "inference");
        assertEquals(1, timer.count());
        assertEquals(250.0, timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS), 1.0);
    }

    @Test
    void testResilienceEvent() {
        // Record resilience events
        metricsService.recordResilienceEvent("circuit_breaker_opened", "python-ai-service");
        metricsService.recordResilienceEvent("retry_attempted", "python-ai-service");
        metricsService.recordResilienceEvent("circuit_breaker_closed", "python-ai-service");

        // Verify resilience event counters
        double openedEvents = meterRegistry.counter("ai_resilience_events_total",
                "event_type", "circuit_breaker_opened",
                "instance", "python-ai-service").count();
        assertEquals(1.0, openedEvents);

        double retryEvents = meterRegistry.counter("ai_resilience_events_total",
                "event_type", "retry_attempted",
                "instance", "python-ai-service").count();
        assertEquals(1.0, retryEvents);

        double closedEvents = meterRegistry.counter("ai_resilience_events_total",
                "event_type", "circuit_breaker_closed",
                "instance", "python-ai-service").count();
        assertEquals(1.0, closedEvents);
    }

    @Test
    void testUnknownCircuitBreakerState() {
        // Test with unknown state - should not change the gauge
        Gauge initialGauge = meterRegistry.find("ai_circuit_breaker_state").gauge();
        assertNotNull(initialGauge);
        double initialState = initialGauge.value();
        
        metricsService.updateCircuitBreakerState("unknown_state");
        
        // State should remain unchanged
        Gauge unchangedGauge = meterRegistry.find("ai_circuit_breaker_state").gauge();
        assertNotNull(unchangedGauge);
        assertEquals(initialState, unchangedGauge.value());
    }

    @Test
    void testMultipleRequestTypes() {
        // Record different types of requests
        metricsService.recordRequest("/ai/parse", "POST");
        metricsService.recordRequest("/ai/prioritize", "POST");
        metricsService.recordRequest("/ai/insights", "GET");
        metricsService.recordRequest("/ai/parse", "POST"); // Duplicate

        // Verify counters for different endpoints
        double parseCount = meterRegistry.counter("ai_service_requests_total",
                "endpoint", "/ai/parse", "method", "POST").count();
        assertEquals(2.0, parseCount);

        double prioritizeCount = meterRegistry.counter("ai_service_requests_total",
                "endpoint", "/ai/prioritize", "method", "POST").count();
        assertEquals(1.0, prioritizeCount);

        double insightsCount = meterRegistry.counter("ai_service_requests_total",
                "endpoint", "/ai/insights", "method", "GET").count();
        assertEquals(1.0, insightsCount);
    }
}