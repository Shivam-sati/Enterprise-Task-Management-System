package com.taskmanagement.gateway.config;

import com.taskmanagement.gateway.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public GatewayConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service Routes (no JWT filter needed)
                .route("auth-service", r -> r.path("/auth/**")
                        .uri("lb://auth-service"))
                
                // Task Service Routes (with JWT authentication)
                .route("task-service", r -> r.path("/tasks/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://task-service"))
                
                // Notification Service Routes (with JWT authentication)
                .route("notification-service", r -> r.path("/notifications/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://notification-service"))
                
                // Collaboration Service Routes (with JWT authentication)
                .route("collaboration-service", r -> r.path("/collaboration/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://collaboration-service"))
                
                // Analytics Service Routes (with JWT authentication)
                .route("analytics-service", r -> r.path("/analytics/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://analytics-service"))
                
                // AI Service Routes (with JWT authentication)
                .route("ai-service", r -> r.path("/ai/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://ai-service"))
                
                .build();
    }
}