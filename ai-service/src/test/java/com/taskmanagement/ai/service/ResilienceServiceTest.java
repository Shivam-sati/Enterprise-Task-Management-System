package com.taskmanagement.ai.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResilienceServiceTest {

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private RetryRegistry retryRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private Retry retry;

    @Mock
    private CircuitBreaker.EventPublisher circuitBreakerEventPublisher;

    @Mock
    private Retry.EventPublisher retryEventPublisher;

    @Mock
    private CircuitBreaker.Metrics metrics;

    private ResilienceService resilienceService;

    @BeforeEach
    void setUp() {
        when(circuitBreakerRegistry.circuitBreaker("python-ai-service")).thenReturn(circuitBreaker);
        when(retryRegistry.retry("python-ai-service")).thenReturn(retry);
        when(circuitBreaker.getEventPublisher()).thenReturn(circuitBreakerEventPublisher);
        when(retry.getEventPublisher()).thenReturn(retryEventPublisher);
        when(circuitBreaker.getMetrics()).thenReturn(metrics);

        // Mock event publisher methods to return themselves for chaining
        when(circuitBreakerEventPublisher.onStateTransition(any())).thenReturn(circuitBreakerEventPublisher);
        when(circuitBreakerEventPublisher.onCallNotPermitted(any())).thenReturn(circuitBreakerEventPublisher);
        when(circuitBreakerEventPublisher.onError(any())).thenReturn(circuitBreakerEventPublisher);
        when(circuitBreakerEventPublisher.onSuccess(any())).thenReturn(circuitBreakerEventPublisher);

        when(retryEventPublisher.onRetry(any())).thenReturn(retryEventPublisher);
        when(retryEventPublisher.onSuccess(any())).thenReturn(retryEventPublisher);
        when(retryEventPublisher.onError(any())).thenReturn(retryEventPublisher);

        resilienceService = new ResilienceService(circuitBreakerRegistry, retryRegistry);
    }

    @Test
    void setupEventListeners_ShouldConfigureEventListeners() {
        // Act
        resilienceService.setupEventListeners();

        // Assert
        verify(circuitBreakerEventPublisher).onStateTransition(any());
        verify(circuitBreakerEventPublisher).onCallNotPermitted(any());
        verify(circuitBreakerEventPublisher).onError(any());
        verify(circuitBreakerEventPublisher).onSuccess(any());

        verify(retryEventPublisher).onRetry(any());
        verify(retryEventPublisher).onSuccess(any());
        verify(retryEventPublisher).onError(any());
    }

    @Test
    void getCircuitBreakerState_ShouldReturnCurrentState() {
        // Arrange
        when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        // Act
        CircuitBreaker.State state = resilienceService.getCircuitBreakerState();

        // Assert
        assertEquals(CircuitBreaker.State.CLOSED, state);
        verify(circuitBreaker).getState();
    }

    @Test
    void getCircuitBreakerMetrics_ShouldReturnMetrics() {
        // Act
        CircuitBreaker.Metrics result = resilienceService.getCircuitBreakerMetrics();

        // Assert
        assertEquals(metrics, result);
        verify(circuitBreaker).getMetrics();
    }
}