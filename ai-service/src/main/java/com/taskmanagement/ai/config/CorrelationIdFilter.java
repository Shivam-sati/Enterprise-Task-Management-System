package com.taskmanagement.ai.config;

import com.taskmanagement.ai.service.CorrelationIdService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to handle correlation ID for distributed tracing
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final CorrelationIdService correlationIdService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Get correlation ID from request header or generate new one
            String correlationId = request.getHeader(CorrelationIdService.CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = correlationIdService.generateCorrelationId();
                log.debug("Generated new correlation ID: {}", correlationId);
            } else {
                log.debug("Using existing correlation ID: {}", correlationId);
            }

            // Set correlation ID in MDC
            correlationIdService.setCorrelationId(correlationId);

            // Add correlation ID to response header
            response.setHeader(CorrelationIdService.CORRELATION_ID_HEADER, correlationId);

            // Log request start
            log.info("Request started: {} {} with correlation ID: {}",
                    request.getMethod(), request.getRequestURI(), correlationId);

            // Continue with the filter chain
            filterChain.doFilter(request, response);

            // Log request completion
            log.info("Request completed: {} {} with status: {} and correlation ID: {}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), correlationId);

        } finally {
            // Clear correlation ID from MDC
            correlationIdService.clearCorrelationId();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip filter for actuator endpoints to avoid noise
        String path = request.getRequestURI();
        return path.startsWith("/actuator/");
    }
}