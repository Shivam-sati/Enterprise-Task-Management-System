package com.taskmanagement.ai.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonAIServiceClient {

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final FallbackService fallbackService;

    @Value("${ai.python.service.name:ai-service-python}")
    private String pythonServiceName;

    @CircuitBreaker(name = "python-ai-service", fallbackMethod = "parseTaskFallback")
    @Retry(name = "python-ai-service")
    @TimeLimiter(name = "python-ai-service")
    public Map<String, Object> parseTask(Map<String, String> request) {
        log.info("Forwarding parse task request to Python AI service");
        
        String serviceUrl = getServiceUrl();
        if (serviceUrl == null) {
            log.warn("Python AI service not available, using fallback");
            return fallbackService.parseTaskFallback(request);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                serviceUrl + "/api/ai/parse-task",
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            log.info("Successfully received response from Python AI service");
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Python AI service for task parsing: {}", e.getMessage());
            throw e; // Let circuit breaker handle this
        }
    }

    @CircuitBreaker(name = "python-ai-service", fallbackMethod = "prioritizeTasksFallback")
    @Retry(name = "python-ai-service")
    @TimeLimiter(name = "python-ai-service")
    public Map<String, Object> prioritizeTasks(Map<String, Object> request) {
        log.info("Forwarding prioritize tasks request to Python AI service");
        
        String serviceUrl = getServiceUrl();
        if (serviceUrl == null) {
            log.warn("Python AI service not available, using fallback");
            return fallbackService.prioritizeTasksFallback(request);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                serviceUrl + "/api/ai/prioritize-tasks",
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            log.info("Successfully received response from Python AI service");
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Python AI service for task prioritization: {}", e.getMessage());
            throw e; // Let circuit breaker handle this
        }
    }

    @CircuitBreaker(name = "python-ai-service", fallbackMethod = "getInsightsFallback")
    @Retry(name = "python-ai-service")
    @TimeLimiter(name = "python-ai-service")
    public Map<String, Object> getProductivityInsights() {
        log.info("Forwarding insights request to Python AI service");
        
        String serviceUrl = getServiceUrl();
        if (serviceUrl == null) {
            log.warn("Python AI service not available, using fallback");
            return fallbackService.getInsightsFallback();
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                serviceUrl + "/api/ai/insights",
                HttpMethod.GET,
                null,
                Map.class
            );
            
            log.info("Successfully received response from Python AI service");
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Python AI service for insights: {}", e.getMessage());
            throw e; // Let circuit breaker handle this
        }
    }

    private String getServiceUrl() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(pythonServiceName);
            if (instances.isEmpty()) {
                log.warn("No instances found for Python AI service: {}", pythonServiceName);
                return null;
            }
            
            ServiceInstance instance = instances.get(0); // Use first available instance
            String url = String.format("http://%s:%d", instance.getHost(), instance.getPort());
            log.debug("Using Python AI service URL: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Error discovering Python AI service: {}", e.getMessage());
            return null;
        }
    }

    // Fallback methods for circuit breaker
    public Map<String, Object> parseTaskFallback(Map<String, String> request, Exception ex) {
        log.warn("Circuit breaker activated for parseTask, using fallback. Error: {}", ex.getMessage());
        return fallbackService.parseTaskFallback(request);
    }

    public Map<String, Object> prioritizeTasksFallback(Map<String, Object> request, Exception ex) {
        log.warn("Circuit breaker activated for prioritizeTasks, using fallback. Error: {}", ex.getMessage());
        return fallbackService.prioritizeTasksFallback(request);
    }

    public Map<String, Object> getInsightsFallback(Exception ex) {
        log.warn("Circuit breaker activated for getInsights, using fallback. Error: {}", ex.getMessage());
        return fallbackService.getInsightsFallback();
    }
}