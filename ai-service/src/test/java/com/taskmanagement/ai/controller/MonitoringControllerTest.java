package com.taskmanagement.ai.controller;

import com.taskmanagement.ai.service.FallbackService;
import com.taskmanagement.ai.service.ResilienceService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MonitoringController.class)
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FallbackService fallbackService;

    @MockBean
    private ResilienceService resilienceService;

    @MockBean
    private DiscoveryClient discoveryClient;

    @MockBean
    private ServiceInstance serviceInstance;

    @MockBean
    private CircuitBreaker.Metrics metrics;

    @Test
    void getServiceStatus_WhenPythonServiceAvailable_ShouldReturnNormalMode() throws Exception {
        // Arrange
        when(discoveryClient.getInstances("ai-service-python")).thenReturn(List.of(serviceInstance));
        when(resilienceService.getCircuitBreakerState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(resilienceService.getCircuitBreakerMetrics()).thenReturn(metrics);
        when(fallbackService.getFallbackUsageCount()).thenReturn(5L);
        when(metrics.getFailureRate()).thenReturn(10.5f);
        when(metrics.getNumberOfBufferedCalls()).thenReturn(20);
        when(metrics.getNumberOfFailedCalls()).thenReturn(2);
        when(metrics.getNumberOfSuccessfulCalls()).thenReturn(18);

        // Act & Assert
        mockMvc.perform(get("/api/ai/monitoring/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pythonServiceInstances").value(1))
            .andExpect(jsonPath("$.pythonServiceAvailable").value(true))
            .andExpect(jsonPath("$.circuitBreakerState").value("CLOSED"))
            .andExpect(jsonPath("$.fallbackUsageCount").value(5))
            .andExpect(jsonPath("$.serviceMode").value("normal"))
            .andExpect(jsonPath("$.circuitBreakerMetrics.failureRate").value("10.50%"))
            .andExpect(jsonPath("$.circuitBreakerMetrics.numberOfCalls").value(20))
            .andExpect(jsonPath("$.circuitBreakerMetrics.numberOfFailedCalls").value(2))
            .andExpect(jsonPath("$.circuitBreakerMetrics.numberOfSuccessfulCalls").value(18));
    }

    @Test
    void getServiceStatus_WhenPythonServiceUnavailable_ShouldReturnDegradedMode() throws Exception {
        // Arrange
        when(discoveryClient.getInstances("ai-service-python")).thenReturn(Collections.emptyList());
        when(resilienceService.getCircuitBreakerState()).thenReturn(CircuitBreaker.State.OPEN);
        when(resilienceService.getCircuitBreakerMetrics()).thenReturn(metrics);
        when(fallbackService.getFallbackUsageCount()).thenReturn(25L);
        when(metrics.getFailureRate()).thenReturn(75.0f);
        when(metrics.getNumberOfBufferedCalls()).thenReturn(10);
        when(metrics.getNumberOfFailedCalls()).thenReturn(8);
        when(metrics.getNumberOfSuccessfulCalls()).thenReturn(2);

        // Act & Assert
        mockMvc.perform(get("/api/ai/monitoring/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pythonServiceInstances").value(0))
            .andExpect(jsonPath("$.pythonServiceAvailable").value(false))
            .andExpect(jsonPath("$.circuitBreakerState").value("OPEN"))
            .andExpect(jsonPath("$.fallbackUsageCount").value(25))
            .andExpect(jsonPath("$.serviceMode").value("degraded"));
    }

    @Test
    void resetFallbackCounter_ShouldResetCounterAndReturnSuccess() throws Exception {
        // Arrange
        doNothing().when(fallbackService).resetFallbackCounter();

        // Act & Assert
        mockMvc.perform(post("/api/ai/monitoring/reset-fallback-counter"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Fallback counter has been reset"));

        verify(fallbackService).resetFallbackCounter();
    }

    @Test
    void resetFallbackCounter_WhenExceptionThrown_ShouldReturnError() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Reset failed")).when(fallbackService).resetFallbackCounter();

        // Act & Assert
        mockMvc.perform(post("/api/ai/monitoring/reset-fallback-counter"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Unable to reset fallback counter"))
            .andExpect(jsonPath("$.message").value("Reset failed"));
    }

    @Test
    void getCircuitBreakerInfo_ShouldReturnDetailedMetrics() throws Exception {
        // Arrange
        when(resilienceService.getCircuitBreakerState()).thenReturn(CircuitBreaker.State.HALF_OPEN);
        when(resilienceService.getCircuitBreakerMetrics()).thenReturn(metrics);
        when(metrics.getFailureRate()).thenReturn(45.5f);
        when(metrics.getSlowCallRate()).thenReturn(20.0f);
        when(metrics.getNumberOfBufferedCalls()).thenReturn(15);
        when(metrics.getNumberOfFailedCalls()).thenReturn(7);
        when(metrics.getNumberOfSuccessfulCalls()).thenReturn(8);
        when(metrics.getNumberOfSlowCalls()).thenReturn(3);
        when(metrics.getNumberOfNotPermittedCalls()).thenReturn(5L);

        // Act & Assert
        mockMvc.perform(get("/api/ai/monitoring/circuit-breaker"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("HALF_OPEN"))
            .andExpect(jsonPath("$.metrics.failureRate").value(45.5))
            .andExpect(jsonPath("$.metrics.slowCallRate").value(20.0))
            .andExpect(jsonPath("$.metrics.numberOfBufferedCalls").value(15))
            .andExpect(jsonPath("$.metrics.numberOfFailedCalls").value(7))
            .andExpect(jsonPath("$.metrics.numberOfSuccessfulCalls").value(8))
            .andExpect(jsonPath("$.metrics.numberOfSlowCalls").value(3))
            .andExpect(jsonPath("$.metrics.numberOfNotPermittedCalls").value(5));
    }

    @Test
    void getServiceStatus_WhenExceptionThrown_ShouldReturnError() throws Exception {
        // Arrange
        when(discoveryClient.getInstances("ai-service-python")).thenThrow(new RuntimeException("Discovery failed"));

        // Act & Assert
        mockMvc.perform(get("/api/ai/monitoring/status"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Unable to retrieve service status"))
            .andExpect(jsonPath("$.message").value("Discovery failed"));
    }
}