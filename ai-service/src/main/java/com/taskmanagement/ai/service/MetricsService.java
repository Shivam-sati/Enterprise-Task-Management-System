package com.taskmanagement.ai.service;

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
 * Service for collecting and exposing custom metrics for AI service
 */
@Service
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;
    
    // Request metrics
    private final Counter requestCounter;
    private final Timer requestTimer;
    
    // AI processing metrics
    private final Counter aiRequestCounter;
    private final Timer aiProcessingTimer;
    private final Counter aiErrorCounter;
    
    // Python service metrics
    private final Counter pythonServiceCallCounter;
    private final Timer pythonServiceCallTimer;
    private final Counter pythonServiceErrorCounter;
    
    // Fallback metrics
    private final Counter fallbackCounter;
    
    // Circuit breaker metrics
    private final AtomicInteger circuitBreakerState = new AtomicInteger(0); // 0=closed, 1=open, 2=half-open
    private final AtomicLong totalPythonServiceCalls = new AtomicLong(0);
    private final AtomicLong successfulPythonServiceCalls = new AtomicLong(0);
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters and timers
        this.requestCounter = Counter.builder("ai_service_requests_total")
                .description("Total number of requests to AI service")
                .tag("service", "ai")
                .register(meterRegistry);
        
        this.requestTimer = Timer.builder("ai_service_request_duration")
                .description("Request processing duration")
                .tag("service", "ai")
                .register(meterRegistry);
        
        this.aiRequestCounter = Counter.builder("ai_processing_requests_total")
                .description("Total number of AI processing requests")
                .tag("service", "ai")
                .register(meterRegistry);
        
        this.aiProcessingTimer = Timer.builder("ai_processing_duration")
                .description("AI processing duration")
                .tag("service", "ai")
                .register(meterRegistry);
        
        this.aiErrorCounter = Counter.builder("ai_processing_errors_total")
                .description("Total number of AI processing errors")
                .tag("service", "ai")
                .register(meterRegistry);
        
        this.pythonServiceCallCounter = Counter.builder("python_service_calls_total")
                .description("Total number of calls to Python AI service")
                .tag("service", "ai")
                .register(meterRegistry);
        
        this.pythonServiceCallTimer = Timer.builder("python_service_call_duration")
                .description("Python AI service call duration")
                .tag("service", "ai")
                .register(meterRegistry);
        
        this.pythonServiceErrorCounter = Counter.builder("python_service_errors_total")
                .description("Total number of Python service errors")
                .tag("service", "ai")
                .register(meterRegistry);
        
        this.fallbackCounter = Counter.builder("ai_fallback_responses_total")
                .description("Total number of fallback responses used")
                .tag("service", "ai")
                .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("ai_circuit_breaker_state", this, MetricsService::getCircuitBreakerState)
                .description("Circuit breaker state (0=closed, 1=open, 2=half-open)")
                .tag("service", "ai")
                .register(meterRegistry);
        
        Gauge.builder("ai_python_service_success_rate", this, MetricsService::getPythonServiceSuccessRate)
                .description("Success rate of Python service calls")
                .tag("service", "ai")
                .register(meterRegistry);
        
        log.info("AI Service MetricsService initialized with custom metrics");
    }
    
    // Request metrics
    public void recordRequest(String endpoint, String method) {
        Counter.builder("ai_service_requests_total")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .register(meterRegistry)
                .increment();
    }
    
    public Timer.Sample startRequestTimer(String endpoint) {
        return Timer.start(meterRegistry);
    }
    
    public void stopRequestTimer(Timer.Sample sample, String endpoint) {
        sample.stop(Timer.builder("ai_service_request_duration")
                .tag("endpoint", endpoint)
                .register(meterRegistry));
    }
    
    // AI processing metrics
    public void recordAIRequest(String requestType) {
        Counter.builder("ai_requests_total")
                .tag("type", requestType)
                .register(meterRegistry)
                .increment();
    }
    
    public Timer.Sample startAIProcessingTimer(String requestType) {
        return Timer.start(meterRegistry);
    }
    
    public void stopAIProcessingTimer(Timer.Sample sample, String requestType) {
        sample.stop(Timer.builder("ai_processing_duration")
                .tag("type", requestType)
                .register(meterRegistry));
    }
    
    public void recordAIError(String requestType, String errorType) {
        Counter.builder("ai_errors_total")
                .tag("type", requestType)
                .tag("error", errorType)
                .register(meterRegistry)
                .increment();
    }
    
    // Python service metrics
    public void recordPythonServiceCall(String endpoint) {
        Counter.builder("python_service_calls_total")
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .increment();
        totalPythonServiceCalls.incrementAndGet();
    }
    
    public Timer.Sample startPythonServiceCallTimer(String endpoint) {
        return Timer.start(meterRegistry);
    }
    
    public void stopPythonServiceCallTimer(Timer.Sample sample, String endpoint, boolean success) {
        sample.stop(Timer.builder("python_service_call_duration")
                .tag("endpoint", endpoint)
                .tag("success", String.valueOf(success))
                .register(meterRegistry));
        
        if (success) {
            successfulPythonServiceCalls.incrementAndGet();
        }
    }
    
    public void recordPythonServiceError(String endpoint, String errorType) {
        Counter.builder("python_service_errors_total")
                .tag("endpoint", endpoint)
                .tag("error", errorType)
                .register(meterRegistry)
                .increment();
    }
    
    // Fallback metrics
    public void recordFallbackUsage(String requestType, String reason) {
        Counter.builder("ai_fallback_usage_total")
                .tag("type", requestType)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }
    
    // Circuit breaker metrics
    public void updateCircuitBreakerState(String state) {
        switch (state.toLowerCase()) {
            case "closed":
                circuitBreakerState.set(0);
                break;
            case "open":
                circuitBreakerState.set(1);
                break;
            case "half_open":
            case "half-open":
                circuitBreakerState.set(2);
                break;
            default:
                log.warn("Unknown circuit breaker state: {}", state);
        }
    }
    
    // Gauge value providers
    private double getCircuitBreakerState() {
        return circuitBreakerState.get();
    }
    
    private double getPythonServiceSuccessRate() {
        long total = totalPythonServiceCalls.get();
        long successful = successfulPythonServiceCalls.get();
        return total > 0 ? (double) successful / total : 0.0;
    }
    
    // Utility methods for monitoring
    public void recordCacheOperation(String operation, boolean hit) {
        Counter.builder("ai_cache_operations_total")
                .description("Total cache operations")
                .tag("operation", operation)
                .tag("hit", String.valueOf(hit))
                .register(meterRegistry)
                .increment();
    }
    
    public void recordModelOperation(String modelType, String operation, Duration duration) {
        Timer.builder("ai_model_operation_duration")
                .description("Model operation duration")
                .tag("model_type", modelType)
                .tag("operation", operation)
                .register(meterRegistry)
                .record(duration);
    }
    
    public void recordResilienceEvent(String eventType, String instanceName) {
        Counter.builder("ai_resilience_events_total")
                .description("Total resilience events")
                .tag("event_type", eventType)
                .tag("instance", instanceName)
                .register(meterRegistry)
                .increment();
    }
}