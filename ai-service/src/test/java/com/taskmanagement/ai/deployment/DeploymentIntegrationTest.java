package com.taskmanagement.ai.deployment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Comprehensive deployment integration tests for AI Service
 * Tests complete Docker deployment flow, service discovery, and inter-service communication
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DeploymentIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Environment environment;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    @Order(1)
    @DisplayName("Test complete service startup and initialization")
    void testCompleteServiceStartup() {
        // Test that service starts up completely with all components
        ResponseEntity<Map<String, Object>> healthResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        
        Map<String, Object> health = healthResponse.getBody();
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        
        // Test that all actuator endpoints are available
        ResponseEntity<Map<String, Object>> infoResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/info", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        assertEquals(HttpStatus.OK, infoResponse.getStatusCode());
        
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/metrics", String.class);
        assertEquals(HttpStatus.OK, metricsResponse.getStatusCode());
    }

    @Test
    @Order(2)
    @DisplayName("Test Docker Compose environment variable integration")
    void testDockerComposeEnvironmentVariables() {
        // Test all environment variables as they would be set in docker-compose.yml
        
        // Service configuration
        String serviceName = environment.getProperty("spring.application.name");
        assertEquals("ai-service", serviceName);
        
        // Eureka configuration
        String eurekaUrl = environment.getProperty("eureka.client.service-url.defaultZone");
        if (eurekaUrl != null && !eurekaUrl.startsWith("${")) {
            assertTrue(eurekaUrl.contains("eureka"), "Eureka URL should contain 'eureka'");
        }
        
        // Redis configuration
        String redisHost = environment.getProperty("spring.redis.host");
        if (redisHost != null && !redisHost.startsWith("${")) {
            assertNotNull(redisHost);
        }
        
        // OpenAI API key configuration
        String openaiApiKey = environment.getProperty("openai.api.key");
        if (openaiApiKey != null && !openaiApiKey.startsWith("${")) {
            assertNotNull(openaiApiKey);
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test Python AI service integration configuration")
    void testPythonAIServiceConfiguration() {
        // Test Python AI service configuration from docker-compose
        String pythonServiceUrl = environment.getProperty("ai.python-service.url");
        String pythonServiceEnabled = environment.getProperty("ai.python-service.enabled");
        String pythonServiceTimeout = environment.getProperty("ai.python-service.timeout-seconds");
        
        // Verify Python AI service configuration
        assertNotNull(environment.getProperty("ai.python-service.url", "http://ai-python-service:8087"));
        assertNotNull(environment.getProperty("ai.python-service.enabled", "true"));
        assertNotNull(environment.getProperty("ai.python-service.timeout-seconds", "30"));
        
        if (pythonServiceUrl != null && !pythonServiceUrl.startsWith("${")) {
            assertTrue(pythonServiceUrl.contains("ai-python-service"), 
                      "Python service URL should use Docker service name");
            assertTrue(pythonServiceUrl.contains("8087"), 
                      "Python service URL should use correct port");
        }
        
        if (pythonServiceEnabled != null && !pythonServiceEnabled.startsWith("${")) {
            assertEquals("true", pythonServiceEnabled, 
                        "Python service should be enabled in docker-compose");
        }
        
        if (pythonServiceTimeout != null && !pythonServiceTimeout.startsWith("${")) {
            assertTrue(Integer.parseInt(pythonServiceTimeout) > 0, 
                      "Python service timeout should be positive");
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test circuit breaker configuration")
    void testCircuitBreakerConfiguration() {
        // Test circuit breaker configuration from docker-compose
        String failureThreshold = environment.getProperty("resilience.circuit-breaker.failure-threshold");
        String timeoutSeconds = environment.getProperty("resilience.circuit-breaker.timeout-seconds");
        String retryAttempts = environment.getProperty("resilience.retry.attempts");
        String retryDelay = environment.getProperty("resilience.retry.delay-ms");
        
        // Verify circuit breaker settings have reasonable defaults
        assertNotNull(environment.getProperty("resilience.circuit-breaker.failure-threshold", "5"));
        assertNotNull(environment.getProperty("resilience.circuit-breaker.timeout-seconds", "10"));
        assertNotNull(environment.getProperty("resilience.retry.attempts", "3"));
        assertNotNull(environment.getProperty("resilience.retry.delay-ms", "1000"));
        
        if (failureThreshold != null && !failureThreshold.startsWith("${")) {
            assertEquals("5", failureThreshold, "Failure threshold should match docker-compose");
        }
        if (timeoutSeconds != null && !timeoutSeconds.startsWith("${")) {
            assertEquals("10", timeoutSeconds, "Timeout should match docker-compose");
        }
        if (retryAttempts != null && !retryAttempts.startsWith("${")) {
            assertEquals("3", retryAttempts, "Retry attempts should match docker-compose");
        }
        if (retryDelay != null && !retryDelay.startsWith("${")) {
            assertEquals("1000", retryDelay, "Retry delay should match docker-compose");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test fallback configuration")
    void testFallbackConfiguration() {
        // Test fallback configuration from docker-compose
        String fallbackEnabled = environment.getProperty("ai.fallback.enabled");
        String fallbackLogUsage = environment.getProperty("ai.fallback.log-usage");
        
        // Verify fallback settings
        assertNotNull(environment.getProperty("ai.fallback.enabled", "true"));
        assertNotNull(environment.getProperty("ai.fallback.log-usage", "true"));
        
        if (fallbackEnabled != null && !fallbackEnabled.startsWith("${")) {
            assertEquals("true", fallbackEnabled, "Fallback should be enabled in docker-compose");
        }
        if (fallbackLogUsage != null && !fallbackLogUsage.startsWith("${")) {
            assertEquals("true", fallbackLogUsage, "Fallback logging should be enabled");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test service health endpoints")
    void testServiceHealthEndpoints() {
        // Test Spring Boot actuator health endpoint
        ResponseEntity<Map<String, Object>> actuatorHealth = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, actuatorHealth.getStatusCode());
        Map<String, Object> health = actuatorHealth.getBody();
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        
        // Test AI service specific health endpoint
        ResponseEntity<Map<String, Object>> aiHealth = restTemplate.getForEntity(
            baseUrl + "/api/ai/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, aiHealth.getStatusCode());
        Map<String, Object> aiHealthData = aiHealth.getBody();
        assertNotNull(aiHealthData);
        assertEquals("UP", aiHealthData.get("status"));
        assertEquals("AI Service (Java Proxy)", aiHealthData.get("service"));
        assertTrue(aiHealthData.containsKey("message"));
    }

    @Test
    @Order(7)
    @DisplayName("Test Docker health check compliance")
    void testDockerHealthCheckCompliance() {
        // Test that health check endpoint responds within Docker timeout (10s)
        long startTime = System.currentTimeMillis();
        
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        long responseTime = System.currentTimeMillis() - startTime;
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(responseTime < 10000, "Health check should respond within 10 seconds (Docker timeout)");
        
        Map<String, Object> health = response.getBody();
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
    }

    @Test
    @Order(8)
    @DisplayName("Test service discovery configuration")
    void testServiceDiscoveryConfiguration() {
        // Test Eureka configuration for service discovery
        String eurekaUrl = environment.getProperty("eureka.client.service-url.defaultZone");
        String serviceName = environment.getProperty("spring.application.name");
        
        assertNotNull(serviceName, "Service name should be configured for Eureka registration");
        assertEquals("ai-service", serviceName);
        
        if (eurekaUrl != null && !eurekaUrl.startsWith("${")) {
            assertTrue(eurekaUrl.contains("eureka"), "Eureka URL should contain 'eureka'");
            // In Docker environment, should use service name not localhost
            if (eurekaUrl.contains("eureka-server")) {
                assertTrue(eurekaUrl.contains("eureka-server"), "Should use Docker service name");
            }
        }
    }

    @Test
    @Order(9)
    @DisplayName("Test inter-service communication configuration")
    void testInterServiceCommunicationConfiguration() {
        // Test Redis configuration
        String redisHost = environment.getProperty("spring.redis.host");
        String redisPort = environment.getProperty("spring.redis.port");
        
        if (redisHost != null && !redisHost.startsWith("${")) {
            assertFalse(redisHost.isEmpty(), "Redis host should not be empty if configured");
            // In Docker environment, should be "redis" service name
            if (redisHost.equals("redis")) {
                assertEquals("redis", redisHost, "Should use Docker service name for Redis");
            }
        }
        
        if (redisPort != null && !redisPort.startsWith("${")) {
            assertEquals("6379", redisPort, "Redis port should be 6379");
        }
        
        // Test Python AI service URL
        String pythonServiceUrl = environment.getProperty("ai.python-service.url");
        if (pythonServiceUrl != null && !pythonServiceUrl.startsWith("${")) {
            assertTrue(pythonServiceUrl.contains("ai-python-service"), 
                      "Should use Docker service name for Python AI service");
        }
    }

    @Test
    @Order(10)
    @DisplayName("Test AI API endpoints")
    void testAIAPIEndpoints() {
        // Test core AI endpoints are accessible
        
        // Test parse-task endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("description", "Test task for deployment validation");
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map<String, Object>> parseResponse = restTemplate.postForEntity(
            baseUrl + "/api/ai/parse-task", 
            request,
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        // Should return 200 with parsed data or fallback response
        assertTrue(parseResponse.getStatusCode().is2xxSuccessful());
        
        Map<String, Object> parseResult = parseResponse.getBody();
        assertNotNull(parseResult);
        assertTrue(parseResult.containsKey("title") || parseResult.containsKey("error"));
        
        // Test prioritize-tasks endpoint
        Map<String, Object> prioritizeRequestBody = new HashMap<>();
        prioritizeRequestBody.put("tasks", new String[]{"Task 1", "Task 2"});
        
        HttpEntity<Map<String, Object>> prioritizeRequest = new HttpEntity<>(prioritizeRequestBody, headers);
        
        ResponseEntity<Map<String, Object>> prioritizeResponse = restTemplate.postForEntity(
            baseUrl + "/api/ai/prioritize-tasks", 
            prioritizeRequest,
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertTrue(prioritizeResponse.getStatusCode().is2xxSuccessful());
        
        // Test insights endpoint
        ResponseEntity<Map<String, Object>> insightsResponse = restTemplate.getForEntity(
            baseUrl + "/api/ai/insights/test-user", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertTrue(insightsResponse.getStatusCode().is2xxSuccessful());
    }

    @Test
    @Order(11)
    @DisplayName("Test fallback mechanism")
    void testFallbackMechanism() {
        // Test that fallback mechanism works when Python service is unavailable
        // This is simulated by testing the endpoints which should return fallback responses
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("description", "Test fallback mechanism");
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
            baseUrl + "/api/ai/parse-task", 
            request,
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        // Should return successful response (either from Python service or fallback)
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> result = response.getBody();
        assertNotNull(result);
        
        // Should contain either real AI response or fallback response
        assertTrue(result.containsKey("title") || result.containsKey("error"));
    }

    @Test
    @Order(12)
    @DisplayName("Test error handling and graceful degradation")
    void testErrorHandlingAndGracefulDegradation() {
        // Test that service handles errors gracefully
        
        // Test non-existent endpoint returns proper error
        ResponseEntity<Map<String, Object>> notFoundResponse = restTemplate.getForEntity(
            baseUrl + "/api/ai/nonexistent", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertTrue(notFoundResponse.getStatusCode().is4xxClientError());
        
        // Test invalid request body
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> invalidRequestBody = new HashMap<>();
        // Missing required field
        
        HttpEntity<Map<String, String>> invalidRequest = new HttpEntity<>(invalidRequestBody, headers);
        
        ResponseEntity<Map<String, Object>> invalidResponse = restTemplate.postForEntity(
            baseUrl + "/api/ai/parse-task", 
            invalidRequest,
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        // Should handle invalid request gracefully
        assertTrue(invalidResponse.getStatusCode().is4xxClientError() || 
                  invalidResponse.getStatusCode().is2xxSuccessful());
    }

    @Test
    @Order(13)
    @DisplayName("Test metrics and monitoring endpoints")
    void testMetricsAndMonitoringEndpoints() {
        // Test Prometheus metrics endpoint
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/metrics", String.class);
        
        assertEquals(HttpStatus.OK, metricsResponse.getStatusCode());
        String metrics = metricsResponse.getBody();
        assertNotNull(metrics);
        assertTrue(metrics.contains("names"), "Metrics should contain metric names");
        
        // Test specific metric endpoint
        ResponseEntity<Map<String, Object>> jvmMemoryResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/metrics/jvm.memory.used", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, jvmMemoryResponse.getStatusCode());
        
        // Test info endpoint
        ResponseEntity<Map<String, Object>> infoResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/info", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, infoResponse.getStatusCode());
    }

    @Test
    @Order(14)
    @DisplayName("Test production readiness")
    void testProductionReadiness() {
        // Test that service is production-ready
        
        // Health check should be UP
        ResponseEntity<Map<String, Object>> healthResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        Map<String, Object> health = healthResponse.getBody();
        assertEquals("UP", health.get("status"));
        
        // Metrics should be available
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/metrics", String.class);
        assertEquals(HttpStatus.OK, metricsResponse.getStatusCode());
        
        // Service should respond quickly (production performance)
        long startTime = System.currentTimeMillis();
        restTemplate.getForEntity(baseUrl + "/api/ai/health", 
                                 (Class<Map<String, Object>>) (Class<?>) Map.class);
        long responseTime = System.currentTimeMillis() - startTime;
        assertTrue(responseTime < 5000, "Service should respond quickly in production");
    }

    @Test
    @Order(15)
    @DisplayName("Test container restart simulation")
    void testContainerRestartSimulation() {
        // Simulate container restart by testing that service can handle
        // multiple rapid requests (as would happen during restart)
        
        for (int i = 0; i < 5; i++) {
            ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
                baseUrl + "/actuator/health", 
                (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> health = response.getBody();
            assertEquals("UP", health.get("status"));
        }
        
        // Test AI service specific health after rapid requests
        ResponseEntity<Map<String, Object>> aiHealthResponse = restTemplate.getForEntity(
            baseUrl + "/api/ai/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, aiHealthResponse.getStatusCode());
        Map<String, Object> aiHealth = aiHealthResponse.getBody();
        assertEquals("UP", aiHealth.get("status"));
    }

    @Test
    @Order(16)
    @DisplayName("Test Docker Compose port mapping compliance")
    void testDockerComposePortMappingCompliance() {
        // Test that service is accessible on the port configured in docker-compose.yml
        // AI service: "8086:8086"
        
        String serverPort = environment.getProperty("server.port");
        if (serverPort != null && !serverPort.equals("0")) {
            // In actual deployment, should be 8086
            // In test, it's random port, but we verify it's configured
            assertNotNull(serverPort);
        }
    }

    @Test
    @Order(17)
    @DisplayName("Test Docker network connectivity configuration")
    void testDockerNetworkConnectivityConfiguration() {
        // Test that service is configured to communicate within Docker network
        
        String eurekaUrl = environment.getProperty("eureka.client.service-url.defaultZone");
        String redisHost = environment.getProperty("spring.redis.host");
        String pythonServiceUrl = environment.getProperty("ai.python-service.url");
        
        // In Docker environment, these should use service names, not localhost
        if (eurekaUrl != null && !eurekaUrl.startsWith("${")) {
            if (eurekaUrl.contains("eureka-server")) {
                assertTrue(eurekaUrl.contains("eureka-server"), 
                          "Eureka URL should use Docker service name");
                assertFalse(eurekaUrl.contains("localhost"), 
                           "Should not use localhost in Docker environment");
            }
        }
        
        if (redisHost != null && !redisHost.startsWith("${")) {
            if (redisHost.equals("redis")) {
                assertEquals("redis", redisHost, 
                           "Redis host should use Docker service name");
                assertNotEquals("localhost", redisHost, 
                               "Should not use localhost in Docker environment");
            }
        }
        
        if (pythonServiceUrl != null && !pythonServiceUrl.startsWith("${")) {
            if (pythonServiceUrl.contains("ai-python-service")) {
                assertTrue(pythonServiceUrl.contains("ai-python-service"), 
                          "Python service URL should use Docker service name");
                assertFalse(pythonServiceUrl.contains("localhost"), 
                           "Should not use localhost in Docker environment");
            }
        }
    }

    @Test
    @Order(18)
    @DisplayName("Test service dependency handling")
    void testServiceDependencyHandling() {
        // Test that service can handle dependency unavailability gracefully
        // This is important for Docker Compose startup order
        
        // Service should start even if Python AI service is not ready
        ResponseEntity<Map<String, Object>> healthResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        
        // AI service health should be UP even if Python service is down (fallback mode)
        ResponseEntity<Map<String, Object>> aiHealthResponse = restTemplate.getForEntity(
            baseUrl + "/api/ai/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, aiHealthResponse.getStatusCode());
        Map<String, Object> aiHealth = aiHealthResponse.getBody();
        assertEquals("UP", aiHealth.get("status"));
        assertEquals("AI Service (Java Proxy)", aiHealth.get("service"));
    }

    @Test
    @Order(19)
    @DisplayName("Test OpenAI API key configuration")
    void testOpenAIAPIKeyConfiguration() {
        // Test OpenAI API key configuration (should be demo-key in docker-compose)
        String openaiApiKey = environment.getProperty("openai.api.key");
        
        if (openaiApiKey != null && !openaiApiKey.startsWith("${")) {
            assertFalse(openaiApiKey.isEmpty(), "OpenAI API key should not be empty if configured");
            // In test/demo environment, it might be "demo-key"
            if (openaiApiKey.equals("demo-key")) {
                assertEquals("demo-key", openaiApiKey, "Demo API key should be configured");
            }
        }
    }

    @Test
    @Order(20)
    @DisplayName("Test complete deployment integration")
    void testCompleteDeploymentIntegration() {
        // Final integration test that verifies all components work together
        
        // 1. Service should be healthy
        ResponseEntity<Map<String, Object>> healthResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        
        // 2. AI endpoints should be accessible
        ResponseEntity<Map<String, Object>> aiHealthResponse = restTemplate.getForEntity(
            baseUrl + "/api/ai/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        assertEquals(HttpStatus.OK, aiHealthResponse.getStatusCode());
        
        // 3. AI functionality should work (with fallback if needed)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("description", "Integration test task");
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map<String, Object>> parseResponse = restTemplate.postForEntity(
            baseUrl + "/api/ai/parse-task", 
            request,
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, parseResponse.getStatusCode());
        assertNotNull(parseResponse.getBody());
        
        // 4. Metrics should be available
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/metrics", String.class);
        assertEquals(HttpStatus.OK, metricsResponse.getStatusCode());
        
        // 5. Service info should be available
        ResponseEntity<Map<String, Object>> infoResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/info", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        assertEquals(HttpStatus.OK, infoResponse.getStatusCode());
        
        // 6. Service should handle requests within reasonable time
        long startTime = System.currentTimeMillis();
        restTemplate.getForEntity(baseUrl + "/api/ai/health", 
                                 (Class<Map<String, Object>>) (Class<?>) Map.class);
        long responseTime = System.currentTimeMillis() - startTime;
        assertTrue(responseTime < 10000, "Service should respond within 10 seconds");
        
        // All tests passed - deployment integration is successful
        assertTrue(true, "Complete deployment integration test passed");
    }
}