package com.taskmanagement.gateway.filter;

import com.taskmanagement.gateway.service.AuthServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final AuthServiceClient authServiceClient;

    @Autowired
    public JwtAuthenticationFilter(AuthServiceClient authServiceClient) {
        super(Config.class);
        this.authServiceClient = authServiceClient;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Skip authentication for auth endpoints and actuator
            if (isAuthEndpoint(request.getPath().toString())) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            
            return authServiceClient.validateToken(token)
                    .flatMap(validationResponse -> {
                        if (validationResponse.isValid()) {
                            // Add user information to request headers for downstream services
                            ServerHttpRequest modifiedRequest = request.mutate()
                                    .header("X-User-Id", validationResponse.getUserId())
                                    .header("X-User-Email", validationResponse.getEmail())
                                    .header("X-User-Role", validationResponse.getRole())
                                    .build();
                            
                            logger.debug("Authentication successful for user: {}", validationResponse.getUserId());
                            return chain.filter(exchange.mutate().request(modifiedRequest).build());
                        } else {
                            return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
                        }
                    })
                    .onErrorResume(error -> {
                        logger.error("Token validation error: {}", error.getMessage());
                        return onError(exchange, "Token validation failed", HttpStatus.UNAUTHORIZED);
                    });
        };
    }

    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/auth/") || 
               path.startsWith("/actuator/") ||
               path.startsWith("/oauth2/") ||
               path.equals("/error");
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        
        // Add error response body
        String body = "{\"error\":\"" + err + "\",\"status\":" + httpStatus.value() + "}";
        org.springframework.core.io.buffer.DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        response.getHeaders().add("Content-Type", "application/json");
        
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Configuration properties if needed
    }
}