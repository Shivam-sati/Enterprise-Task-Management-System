package com.taskmanagement.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AuthServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceClient.class);
    
    private final WebClient webClient;
    
    @Autowired
    public AuthServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://auth-service")
                .build();
    }
    
    public Mono<TokenValidationResponse> validateToken(String token) {
        return webClient.get()
                .uri("/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    logger.error("Token validation failed with status: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Token validation failed"));
                })
                .bodyToMono(TokenValidationResponse.class)
                .doOnSuccess(response -> logger.debug("Token validated successfully for user: {}", response.getUserId()))
                .doOnError(error -> logger.error("Error validating token: {}", error.getMessage()));
    }
    
    public static class TokenValidationResponse {
        private boolean valid;
        private String userId;
        private String email;
        private String role;
        
        public TokenValidationResponse() {}
        
        // Getters and Setters
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
    }
}