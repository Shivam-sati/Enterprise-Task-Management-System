package com.taskmanagement.analytics.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for managing correlation IDs for distributed tracing
 */
@Service
@Slf4j
public class CorrelationIdService {

    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    /**
     * Generate a new correlation ID
     */
    public String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Set correlation ID in MDC for current thread
     */
    public void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            MDC.put(CORRELATION_ID_KEY, correlationId);
            log.debug("Set correlation ID: {}", correlationId);
        }
    }

    /**
     * Get correlation ID from MDC
     */
    public String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    /**
     * Clear correlation ID from MDC
     */
    public void clearCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (correlationId != null) {
            log.debug("Clearing correlation ID: {}", correlationId);
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    /**
     * Get or generate correlation ID
     */
    public String getOrGenerateCorrelationId() {
        String correlationId = getCorrelationId();
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = generateCorrelationId();
            setCorrelationId(correlationId);
        }
        return correlationId;
    }

    /**
     * Execute a runnable with correlation ID context
     */
    public void executeWithCorrelationId(String correlationId, Runnable runnable) {
        String previousCorrelationId = getCorrelationId();
        try {
            setCorrelationId(correlationId);
            runnable.run();
        } finally {
            if (previousCorrelationId != null) {
                setCorrelationId(previousCorrelationId);
            } else {
                clearCorrelationId();
            }
        }
    }
}