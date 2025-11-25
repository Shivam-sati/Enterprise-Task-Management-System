package com.taskmanagement.ai.controller;

import com.taskmanagement.ai.service.FallbackService;
import com.taskmanagement.ai.service.ResilienceService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/monitoring")
@RequiredArgsConstructor
@Slf4j
public class MonitoringController {

    private final FallbackService fallbackService;
    private final ResilienceService resilienceService;
    private final DiscoveryClient discoveryClient;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances("ai-service-python");
            CircuitBreaker.State circuitBreakerState = resilienceService.getCircuitBreakerState();
            CircuitBreaker.Metrics metrics = resilienceService.getCircuitBreakerMetrics();
            
            return ResponseEntity.ok(Map.of(
                "pythonServiceInstances", instances.size(),
                "pythonServiceAvailable", !instances.isEmpty(),
                "circuitBreakerState", circuitBreakerState.toString(),
                "fallbackUsageCount", fallbackService.getFallbackUsageCount(),
                "circuitBreakerMetrics", Map.of(
                    "failureRate", String.format("%.2f%%", metrics.getFailureRate()),
                    "numberOfCalls", metrics.getNumberOfBufferedCalls(),
                    "numberOfFailedCalls", metrics.getNumberOfFailedCalls(),
                    "numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls()
                ),
                "serviceMode", circuitBreakerState == CircuitBreaker.State.CLOSED ? "normal" : "degraded"
            ));
        } catch (Exception e) {
            log.error("Error getting service status: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Unable to retrieve service status", "message", e.getMessage()));
        }
    }

    @PostMapping("/reset-fallback-counter")
    public ResponseEntity<Map<String, String>> resetFallbackCounter() {
        try {
            fallbackService.resetFallbackCounter();
            log.info("Fallback counter reset via monitoring endpoint");
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Fallback counter has been reset"
            ));
        } catch (Exception e) {
            log.error("Error resetting fallback counter: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Unable to reset fallback counter", "message", e.getMessage()));
        }
    }

    @GetMapping("/circuit-breaker")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerInfo() {
        try {
            CircuitBreaker.State state = resilienceService.getCircuitBreakerState();
            CircuitBreaker.Metrics metrics = resilienceService.getCircuitBreakerMetrics();
            
            return ResponseEntity.ok(Map.of(
                "state", state.toString(),
                "metrics", Map.of(
                    "failureRate", metrics.getFailureRate(),
                    "slowCallRate", metrics.getSlowCallRate(),
                    "numberOfBufferedCalls", metrics.getNumberOfBufferedCalls(),
                    "numberOfFailedCalls", metrics.getNumberOfFailedCalls(),
                    "numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls(),
                    "numberOfSlowCalls", metrics.getNumberOfSlowCalls(),
                    "numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls()
                )
            ));
        } catch (Exception e) {
            log.error("Error getting circuit breaker info: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Unable to retrieve circuit breaker information", "message", e.getMessage()));
        }
    }
}