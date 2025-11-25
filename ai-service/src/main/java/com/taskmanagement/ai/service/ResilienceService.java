package com.taskmanagement.ai.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResilienceService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    @PostConstruct
    public void setupEventListeners() {
        setupCircuitBreakerEventListeners();
        setupRetryEventListeners();
    }

    private void setupCircuitBreakerEventListeners() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("python-ai-service");
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("Circuit breaker state transition: {} -> {} for service: python-ai-service", 
                    event.getStateTransition().getFromState(), 
                    event.getStateTransition().getToState())
            )
            .onCallNotPermitted(event -> 
                log.warn("Circuit breaker call not permitted for service: python-ai-service")
            )
            .onError(event -> 
                log.error("Circuit breaker error for service: python-ai-service - Duration: {}ms, Error: {}", 
                    event.getElapsedDuration().toMillis(), 
                    event.getThrowable().getMessage())
            )
            .onSuccess(event -> 
                log.debug("Circuit breaker success for service: python-ai-service - Duration: {}ms", 
                    event.getElapsedDuration().toMillis())
            );
    }

    private void setupRetryEventListeners() {
        Retry retry = retryRegistry.retry("python-ai-service");
        
        retry.getEventPublisher()
            .onRetry(event -> 
                log.warn("Retry attempt {} for service: python-ai-service - Error: {}", 
                    event.getNumberOfRetryAttempts(), 
                    event.getLastThrowable().getMessage())
            )
            .onSuccess(event -> 
                log.info("Retry successful after {} attempts for service: python-ai-service", 
                    event.getNumberOfRetryAttempts())
            )
            .onError(event -> 
                log.error("Retry failed after {} attempts for service: python-ai-service - Final error: {}", 
                    event.getNumberOfRetryAttempts(), 
                    event.getLastThrowable().getMessage())
            );
    }

    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreakerRegistry.circuitBreaker("python-ai-service").getState();
    }

    public CircuitBreaker.Metrics getCircuitBreakerMetrics() {
        return circuitBreakerRegistry.circuitBreaker("python-ai-service").getMetrics();
    }
}