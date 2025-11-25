package com.taskmanagement.auth.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Configuration
public class LoggingConfig {

    @Bean
    public OncePerRequestFilter correlationIdFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                          FilterChain filterChain) throws ServletException, IOException {
                try {
                    String correlationId = request.getHeader("X-Correlation-ID");
                    if (correlationId == null || correlationId.isEmpty()) {
                        correlationId = UUID.randomUUID().toString();
                    }
                    
                    MDC.put("correlationId", correlationId);
                    MDC.put("service", "auth-service");
                    MDC.put("requestUri", request.getRequestURI());
                    MDC.put("method", request.getMethod());
                    
                    response.setHeader("X-Correlation-ID", correlationId);
                    
                    filterChain.doFilter(request, response);
                } finally {
                    MDC.clear();
                }
            }
        };
    }
}