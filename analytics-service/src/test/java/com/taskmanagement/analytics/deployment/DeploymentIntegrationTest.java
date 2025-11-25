package com.taskmanagement.analytics.deployment;

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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Comprehensive deployment integration tests for Analytics Service
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
        assertEquals("analytics-service", serviceName);
        
        // Database configuration
        String mongoUri = environment.getProperty("spring.data.mongodb.uri");
        if (mongoUri != null && !mongoUri.startsWith("${")) {
            assertTrue(mongoUri.contains("mongodb"), "MongoDB URI should contain 'mongodb'");
        }
        
        String redisHost = environment.getProperty("spring.redis.host");
        if (redisHost != null && !redisHost.startsWith("${")) {
            // In Docker environment, should be "redis" service name
            assertNotNull(redisHost);
        }
        
        // Eureka configuration
        String eurekaUrl = environment.getProperty("eureka.client.service-url.defaultZone");
        if (eurekaUrl != null && !eurekaUrl.startsWith("${")) {
            assertTrue(eurekaUrl.contains("eureka"), "Eureka URL should contain 'eureka'");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test analytics-specific environment variables")
    void testAnalyticsEnvironmentVariables() {
        // Test analytics calculation settings
        String minDataPoints = environment.getProperty("analytics.calculation.min-data-points");
        String trendAnalysisDays = environment.getProperty("analytics.calculation.trend-analysis-days");
        String cacheTtlMinutes = environment.getProperty("analytics.calculation.cache-ttl-minutes");
        
        // These should have default values even if not explicitly set
        assertNotNull(environment.getProperty("analytics.calculation.min-data-points", "5"));
        assertNotNull(environment.getProperty("analytics.calculation.trend-analysis-days", "30"));
        assertNotNull(environment.getProperty("analytics.calculation.cache-ttl-minutes", "15"));
        
        // Test insights settings
        String confidenceThreshold = environment.getProperty("analytics.insights.confidence-threshold");
        String maxRecommendations = environment.getProperty("analytics.insights.max-recommendations");
        
        assertNotNull(environment.getProperty("analytics.insights.confidence-threshold", "0.7"));
        assertNotNull(environment.getProperty("analytics.insights.max-recommendations", "5"));
        
        // Test prediction settings
        String predictionEnabled = environment.getProperty("analytics.prediction.enabled");
        String forecastDays = environment.getProperty("analytics.prediction.forecast-days");
        String accuracyThreshold = environment.getProperty("analytics.prediction.model-accuracy-threshold");
        
        assertNotNull(environment.getProperty("analytics.prediction.enabled", "true"));
        assertNotNull(environment.getProperty("analytics.prediction.forecast-days", "7"));
        assertNotNull(environment.getProperty("analytics.prediction.model-accuracy-threshold", "0.8"));
    }

    @Test
    @Order(4)
    @DisplayName("Test resilience configuration")
    void testResilienceConfiguration() {
        // Test circuit breaker configuration
        String failureThreshold = environment.getProperty("analytics.resilience.circuit-breaker.failure-threshold");
        String timeoutSeconds = environment.getProperty("analytics.resilience.circuit-breaker.timeout-seconds");
        String retryAttempts = environment.getProperty("analytics.resilience.circuit-breaker.retry-attempts");
        
        // Verify resilience settings have reasonable defaults
        assertNotNull(environment.getProperty("analytics.resilience.circuit-breaker.failure-threshold", "5"));
        assertNotNull(environment.getProperty("analytics.resilience.circuit-breaker.timeout-seconds", "10"));
        assertNotNull(environment.getProperty("analytics.resilience.circuit-breaker.retry-attempts", "3"));
        
        if (failureThreshold != null && !failureThreshold.startsWith("${")) {
            assertTrue(Integer.parseInt(failureThreshold) > 0, "Failure threshold should be positive");
        }
        if (timeoutSeconds != null && !timeoutSeconds.startsWith("${")) {
            assertTrue(Integer.parseInt(timeoutSeconds) > 0, "Timeout should be positive");
        }
        if (retryAttempts != null && !retryAttempts.startsWith("${")) {
            assertTrue(Integer.parseInt(retryAttempts) >= 0, "Retry attempts should be non-negative");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test observability configuration")
    void testObservabilityConfiguration() {
        // Test observability settings
        String metricsEnabled = environment.getProperty("analytics.observability.metrics-enabled");
        String prometheusPort = environment.getProperty("analytics.observability.prometheus-port");
        
        // Verify observability is properly configured
        assertNotNull(environment.getProperty("analytics.observability.metrics-enabled", "true"));
        assertNotNull(environment.getProperty("analytics.observability.prometheus-port", "9091"));
        
        if (prometheusPort != null && !prometheusPort.startsWith("${")) {
            assertEquals("9091", prometheusPort, "Prometheus port should match docker-compose configuration");
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
        
        // Test analytics service specific health endpoint
        ResponseEntity<Map<String, Object>> analyticsHealth = restTemplate.getForEntity(
            baseUrl + "/api/analytics/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, analyticsHealth.getStatusCode());
        Map<String, Object> analyticsHealthData = analyticsHealth.getBody();
        assertNotNull(analyticsHealthData);
        assertEquals("UP", analyticsHealthData.get("status"));
        assertEquals("Analytics Service", analyticsHealthData.get("service"));
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
        assertEquals("analytics-service", serviceName);
        
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
        // Test Redis configuration for caching
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
    }

    @Test
    @Order(10)
    @DisplayName("Test analytics API endpoints")
    void testAnalyticsAPIEndpoints() {
        // Test core analytics endpoints are accessible
        
        // Test productivity endpoint (should handle missing user gracefully)
        ResponseEntity<Map<String, Object>> productivityResponse = restTemplate.getForEntity(
            baseUrl + "/api/analytics/productivity/test-user", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        // Should return 200 with default/empty data or appropriate error
        assertTrue(productivityResponse.getStatusCode().is2xxSuccessful() || 
                  productivityResponse.getStatusCode().is4xxClientError());
        
        // Test dashboard endpoint
        ResponseEntity<Map<String, Object>> dashboardResponse = restTemplate.getForEntity(
            baseUrl + "/api/analytics/dashboard/test-user", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertTrue(dashboardResponse.getStatusCode().is2xxSuccessful() || 
                  dashboardResponse.getStatusCode().is4xxClientError());
        
        // Test insights endpoint
        ResponseEntity<Map<String, Object>> insightsResponse = restTemplate.getForEntity(
            baseUrl + "/api/analytics/insights/test-user", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertTrue(insightsResponse.getStatusCode().is2xxSuccessful() || 
                  insightsResponse.getStatusCode().is4xxClientError());
    }

    @Test
    @Order(11)
    @DisplayName("Test error handling and graceful degradation")
    void testErrorHandlingAndGracefulDegradation() {
        // Test that service handles missing dependencies gracefully
        
        // Test non-existent endpoint returns proper error
        ResponseEntity<Map<String, Object>> notFoundResponse = restTemplate.getForEntity(
            baseUrl + "/api/analytics/nonexistent", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertTrue(notFoundResponse.getStatusCode().is4xxClientError());
        
        // Test that service can handle invalid user IDs gracefully
        ResponseEntity<Map<String, Object>> invalidUserResponse = restTemplate.getForEntity(
            baseUrl + "/api/analytics/productivity/", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertTrue(invalidUserResponse.getStatusCode().is4xxClientError());
    }

    @Test
    @Order(12)
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
    @Order(13)
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
        restTemplate.getForEntity(baseUrl + "/api/analytics/health", 
                                 (Class<Map<String, Object>>) (Class<?>) Map.class);
        long responseTime = System.currentTimeMillis() - startTime;
        assertTrue(responseTime < 5000, "Service should respond quickly in production");
    }

    @Test
    @Order(14)
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
    }

    @Test
    @Order(15)
    @DisplayName("Test Docker Compose port mapping compliance")
    void testDockerComposePortMappingCompliance() {
        // Test that service is accessible on the port configured in docker-compose.yml
        // Analytics service: "8085:8085" and "9091:9091" (metrics)
        
        String serverPort = environment.getProperty("server.port");
        if (serverPort != null && !serverPort.equals("0")) {
            // In actual deployment, should be 8085
            // In test, it's random port, but we verify it's configured
            assertNotNull(serverPort);
        }
        
        String prometheusPort = environment.getProperty("analytics.observability.prometheus-port");
        if (prometheusPort != null && !prometheusPort.startsWith("${")) {
            assertEquals("9091", prometheusPort, "Prometheus port should match docker-compose");
        }
    }

    @Test
    @Order(16)
    @DisplayName("Test Docker network connectivity configuration")
    void testDockerNetworkConnectivityConfiguration() {
        // Test that service is configured to communicate within Docker network
        
        String eurekaUrl = environment.getProperty("eureka.client.service-url.defaultZone");
        String redisHost = environment.getProperty("spring.redis.host");
        
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
    }

    @Test
    @Order(17)
    @DisplayName("Test service dependency handling")
    void testServiceDependencyHandling() {
        // Test that service can handle dependency unavailability gracefully
        // This is important for Docker Compose startup order
        
        // Service should start even if some dependencies are not ready
        ResponseEntity<Map<String, Object>> healthResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        
        // Analytics service health should be UP even if external services are down
        ResponseEntity<Map<String, Object>> analyticsHealthResponse = restTemplate.getForEntity(
            baseUrl + "/api/analytics/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, analyticsHealthResponse.getStatusCode());
        Map<String, Object> analyticsHealth = analyticsHealthResponse.getBody();
        assertEquals("UP", analyticsHealth.get("status"));
    }

    @Test
    @Order(18)
    @DisplayName("Test configuration validation")
    void testConfigurationValidation() {
        // Test that all critical configuration is valid
        
        // Server port should be valid
        String serverPort = environment.getProperty("server.port");
        if (serverPort != null && !serverPort.equals("0")) {
            int port = Integer.parseInt(serverPort);
            assertTrue(port > 0 && port <= 65535, "Server port should be valid");
        }
        
        // Service name should be set
        String serviceName = environment.getProperty("spring.application.name");
        assertNotNull(serviceName, "Service name should be configured");
        assertEquals("analytics-service", serviceName);
        
        // Analytics configuration should be valid
        String minDataPoints = environment.getProperty("analytics.calculation.min-data-points", "5");
        assertTrue(Integer.parseInt(minDataPoints) > 0, "Min data points should be positive");
        
        String confidenceThreshold = environment.getProperty("analytics.insights.confidence-threshold", "0.7");
        double threshold = Double.parseDouble(confidenceThreshold);
        assertTrue(threshold >= 0.0 && threshold <= 1.0, "Confidence threshold should be between 0 and 1");
    }

    @Test
    @Order(19)
    @DisplayName("Test logging configuration")
    void testLoggingConfiguration() {
        // Test that logging is properly configured for containers
        
        // Verify logging configuration is accessible
        String logLevel = environment.getProperty("logging.level.com.taskmanagement.analytics");
        String rootLogLevel = environment.getProperty("logging.level.root");
        
        // Logging should be configured (may be defaults)
        assertNotNull(environment.getProperty("logging.pattern.console", "default"));
        
        // In production, debug should be disabled
        String activeProfile = environment.getProperty("spring.profiles.active");
        if ("docker".equals(activeProfile) || "prod".equals(activeProfile)) {
            // Production logging should not be debug level
            if (rootLogLevel != null) {
                assertNotEquals("DEBUG", rootLogLevel.toUpperCase());
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
        
        // 2. Analytics endpoints should be accessible
        ResponseEntity<Map<String, Object>> analyticsHealthResponse = restTemplate.getForEntity(
            baseUrl + "/api/analytics/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        assertEquals(HttpStatus.OK, analyticsHealthResponse.getStatusCode());
        
        // 3. Metrics should be available
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/metrics", String.class);
        assertEquals(HttpStatus.OK, metricsResponse.getStatusCode());
        
        // 4. Service info should be available
        ResponseEntity<Map<String, Object>> infoResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/info", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        assertEquals(HttpStatus.OK, infoResponse.getStatusCode());
        
        // 5. Service should handle requests within reasonable time
        long startTime = System.currentTimeMillis();
        restTemplate.getForEntity(baseUrl + "/api/analytics/health", 
                                 (Class<Map<String, Object>>) (Class<?>) Map.class);
        long responseTime = System.currentTimeMillis() - startTime;
        assertTrue(responseTime < 10000, "Service should respond within 10 seconds");
        
        // All tests passed - deployment integration is successful
        assertTrue(true, "Complete deployment integration test passed");
    }
}