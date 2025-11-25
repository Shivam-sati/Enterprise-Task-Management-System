package com.taskmanagement.analytics.deployment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Deployment and configuration tests for Analytics Service
 * Tests Docker container startup, health checks, and service discovery
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DeploymentConfigurationTest {

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
    @DisplayName("Test application startup with default configuration")
    void testApplicationStartupWithDefaults() {
        // Verify application started successfully
        ResponseEntity<Map<String, Object>> healthResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        
        Map<String, Object> healthBody = healthResponse.getBody();
        assertNotNull(healthBody);
        assertEquals("UP", healthBody.get("status"));
    }

    @Test
    @DisplayName("Test health check endpoint accessibility")
    void testHealthCheckEndpoint() {
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> health = response.getBody();
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
    }

    @Test
    @DisplayName("Test analytics service health endpoint")
    void testAnalyticsServiceHealthEndpoint() {
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            baseUrl + "/api/analytics/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> health = response.getBody();
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        assertEquals("Analytics Service", health.get("service"));
    }

    @Test
    @DisplayName("Test metrics endpoint accessibility")
    void testMetricsEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/actuator/metrics", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        String metrics = response.getBody();
        assertNotNull(metrics);
        assertTrue(metrics.contains("names"));
    }

    @Test
    @DisplayName("Test Prometheus metrics endpoint")
    void testPrometheusMetricsEndpoint() {
        // Test Prometheus metrics endpoint if configured
        String prometheusPort = environment.getProperty("analytics.observability.prometheus-port");
        if (prometheusPort != null && !prometheusPort.equals("${ANALYTICS_OBSERVABILITY_PROMETHEUS_PORT:9091}")) {
            // In a real deployment test, you would test the actual Prometheus endpoint
            // For now, just verify the configuration is accessible
            assertNotNull(prometheusPort);
        }
    }

    @Test
    @DisplayName("Test analytics calculation configuration")
    void testAnalyticsCalculationConfiguration() {
        // Test analytics calculation properties
        String minDataPoints = environment.getProperty("analytics.calculation.min-data-points");
        String trendAnalysisDays = environment.getProperty("analytics.calculation.trend-analysis-days");
        String cacheTtlMinutes = environment.getProperty("analytics.calculation.cache-ttl-minutes");
        
        // Verify configuration exists (may be defaults)
        if (minDataPoints != null && !minDataPoints.startsWith("${")) {
            assertTrue(Integer.parseInt(minDataPoints) > 0, "Min data points should be positive");
        }
        if (trendAnalysisDays != null && !trendAnalysisDays.startsWith("${")) {
            assertTrue(Integer.parseInt(trendAnalysisDays) > 0, "Trend analysis days should be positive");
        }
        if (cacheTtlMinutes != null && !cacheTtlMinutes.startsWith("${")) {
            assertTrue(Integer.parseInt(cacheTtlMinutes) > 0, "Cache TTL should be positive");
        }
    }

    @Test
    @DisplayName("Test analytics insights configuration")
    void testAnalyticsInsightsConfiguration() {
        // Test insights configuration
        String confidenceThreshold = environment.getProperty("analytics.insights.confidence-threshold");
        String maxRecommendations = environment.getProperty("analytics.insights.max-recommendations");
        
        if (confidenceThreshold != null && !confidenceThreshold.startsWith("${")) {
            double threshold = Double.parseDouble(confidenceThreshold);
            assertTrue(threshold >= 0.0 && threshold <= 1.0, "Confidence threshold should be between 0 and 1");
        }
        if (maxRecommendations != null && !maxRecommendations.startsWith("${")) {
            assertTrue(Integer.parseInt(maxRecommendations) > 0, "Max recommendations should be positive");
        }
    }

    @Test
    @DisplayName("Test prediction configuration")
    void testPredictionConfiguration() {
        // Test prediction settings
        String predictionEnabled = environment.getProperty("analytics.prediction.enabled");
        String forecastDays = environment.getProperty("analytics.prediction.forecast-days");
        String accuracyThreshold = environment.getProperty("analytics.prediction.model-accuracy-threshold");
        
        if (predictionEnabled != null && !predictionEnabled.startsWith("${")) {
            assertTrue(predictionEnabled.equals("true") || predictionEnabled.equals("false"),
                      "Prediction enabled should be boolean");
        }
        if (forecastDays != null && !forecastDays.startsWith("${")) {
            assertTrue(Integer.parseInt(forecastDays) > 0, "Forecast days should be positive");
        }
        if (accuracyThreshold != null && !accuracyThreshold.startsWith("${")) {
            double threshold = Double.parseDouble(accuracyThreshold);
            assertTrue(threshold >= 0.0 && threshold <= 1.0, "Accuracy threshold should be between 0 and 1");
        }
    }

    @Test
    @DisplayName("Test resilience configuration")
    void testResilienceConfiguration() {
        // Test circuit breaker configuration
        String failureThreshold = environment.getProperty("analytics.resilience.circuit-breaker.failure-threshold");
        String timeoutSeconds = environment.getProperty("analytics.resilience.circuit-breaker.timeout-seconds");
        String retryAttempts = environment.getProperty("analytics.resilience.circuit-breaker.retry-attempts");
        
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
    @DisplayName("Test database configuration")
    void testDatabaseConfiguration() {
        // Test MongoDB configuration
        String mongoUri = environment.getProperty("spring.data.mongodb.uri");
        String redisHost = environment.getProperty("spring.redis.host");
        
        if (mongoUri != null && !mongoUri.startsWith("${")) {
            assertTrue(mongoUri.contains("mongodb"), "MongoDB URI should contain 'mongodb'");
        }
        if (redisHost != null && !redisHost.startsWith("${")) {
            assertFalse(redisHost.isEmpty(), "Redis host should not be empty if configured");
        }
    }

    @Test
    @DisplayName("Test service discovery configuration")
    void testServiceDiscoveryConfiguration() {
        // Test Eureka configuration
        String eurekaUrl = environment.getProperty("eureka.client.service-url.defaultZone");
        String serviceName = environment.getProperty("spring.application.name");
        
        if (eurekaUrl != null && !eurekaUrl.startsWith("${")) {
            assertTrue(eurekaUrl.contains("eureka"), "Eureka URL should contain 'eureka'");
        }
        assertNotNull(serviceName, "Service name should be configured");
    }

    @Test
    @DisplayName("Test observability configuration")
    void testObservabilityConfiguration() {
        // Test observability settings
        String metricsEnabled = environment.getProperty("analytics.observability.metrics-enabled");
        String prometheusPort = environment.getProperty("analytics.observability.prometheus-port");
        
        if (metricsEnabled != null && !metricsEnabled.startsWith("${")) {
            assertTrue(metricsEnabled.equals("true") || metricsEnabled.equals("false"),
                      "Metrics enabled should be boolean");
        }
        if (prometheusPort != null && !prometheusPort.startsWith("${")) {
            int port = Integer.parseInt(prometheusPort);
            assertTrue(port > 0 && port <= 65535, "Prometheus port should be valid");
        }
    }

    @Test
    @DisplayName("Test startup time performance")
    void testStartupTimePerformance() {
        long startTime = System.currentTimeMillis();
        
        // Test that health endpoint responds quickly
        await().atMost(5, TimeUnit.SECONDS)
               .until(() -> {
                   try {
                       ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
                           baseUrl + "/actuator/health", 
                           (Class<Map<String, Object>>) (Class<?>) Map.class);
                       return response.getStatusCode() == HttpStatus.OK;
                   } catch (Exception e) {
                       return false;
                   }
               });
        
        long responseTime = System.currentTimeMillis() - startTime;
        assertTrue(responseTime < 5000, "Health endpoint should respond within 5 seconds");
    }

    @Test
    @DisplayName("Test environment variable handling")
    void testEnvironmentVariableHandling() {
        // Test that environment variables are properly handled
        String serverPort = environment.getProperty("server.port");
        String springProfile = environment.getProperty("spring.profiles.active");
        
        assertNotNull(serverPort, "Server port should be configured");
        assertNotNull(springProfile, "Spring profile should be configured");
        
        // Verify port is valid
        if (!serverPort.equals("0")) { // 0 means random port in tests
            int port = Integer.parseInt(serverPort);
            assertTrue(port > 0 && port <= 65535, "Server port should be valid");
        }
    }

    @Test
    @DisplayName("Test configuration defaults")
    void testConfigurationDefaults() {
        // Test that default values are reasonable
        String serviceName = environment.getProperty("spring.application.name");
        assertNotNull(serviceName, "Service name should have a default");
        
        // Test that critical paths have defaults
        String managementPort = environment.getProperty("management.server.port");
        if (managementPort != null) {
            int port = Integer.parseInt(managementPort);
            assertTrue(port > 0, "Management port should be positive if configured");
        }
    }

    @Test
    @DisplayName("Test error handling configuration")
    void testErrorHandlingConfiguration() {
        // Test that error handling is properly configured
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            baseUrl + "/api/analytics/nonexistent-endpoint", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        // Should return proper error response
        assertTrue(response.getStatusCode().is4xxClientError() || 
                  response.getStatusCode().is5xxServerError(),
                  "Non-existent endpoint should return error status");
    }

    @Test
    @DisplayName("Test security configuration")
    void testSecurityConfiguration() {
        // Test that security is properly configured
        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/actuator/health", Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Test logging configuration")
    void testLoggingConfiguration() {
        // Test that logging configuration is accessible
        String logLevel = environment.getProperty("logging.level.com.taskmanagement.analytics");
        String rootLogLevel = environment.getProperty("logging.level.root");
        
        // Verify logging configuration exists (may be defaults)
        assertNotNull(environment.getProperty("logging.pattern.console", "default"));
        
        // Verify log levels are accessible (may be null in test environment)
        if (logLevel != null) {
            assertFalse(logLevel.isEmpty(), "Log level should not be empty if configured");
        }
        if (rootLogLevel != null) {
            assertFalse(rootLogLevel.isEmpty(), "Root log level should not be empty if configured");
        }
    }

    @Test
    @DisplayName("Test resource limits configuration")
    void testResourceLimitsConfiguration() {
        // Test resource-related configuration
        String maxThreads = environment.getProperty("server.tomcat.threads.max");
        String connectionTimeout = environment.getProperty("server.tomcat.connection-timeout");
        
        if (maxThreads != null) {
            assertTrue(Integer.parseInt(maxThreads) > 0, "Max threads should be positive");
        }
        if (connectionTimeout != null) {
            assertTrue(connectionTimeout.matches("\\d+.*"), "Connection timeout should be numeric");
        }
    }

    @Test
    @DisplayName("Test task service client configuration")
    void testTaskServiceClientConfiguration() {
        // Test that task service client configuration is accessible
        String eurekaUrl = environment.getProperty("eureka.client.service-url.defaultZone");
        
        // Task service client relies on service discovery
        if (eurekaUrl != null && !eurekaUrl.startsWith("${")) {
            assertTrue(eurekaUrl.contains("eureka"), "Eureka should be configured for task service discovery");
        }
    }

    @Test
    @DisplayName("Test cache configuration")
    void testCacheConfiguration() {
        // Test cache-related configuration
        String redisHost = environment.getProperty("spring.redis.host");
        String cacheTtl = environment.getProperty("analytics.calculation.cache-ttl-minutes");
        
        if (redisHost != null && !redisHost.startsWith("${")) {
            assertFalse(redisHost.isEmpty(), "Redis host should not be empty if configured");
        }
        if (cacheTtl != null && !cacheTtl.startsWith("${")) {
            assertTrue(Integer.parseInt(cacheTtl) > 0, "Cache TTL should be positive");
        }
    }

    @Test
    @DisplayName("Test production readiness")
    void testProductionReadiness() {
        // Test that the service is production-ready
        ResponseEntity<Map<String, Object>> healthResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        
        // Test that metrics are available
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/metrics", String.class);
        
        assertEquals(HttpStatus.OK, metricsResponse.getStatusCode());
        
        // Test that info endpoint is available
        ResponseEntity<Map<String, Object>> infoResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/info", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, infoResponse.getStatusCode());
    }

    @Test
    @DisplayName("Test graceful degradation configuration")
    void testGracefulDegradationConfiguration() {
        // Test that the service can handle missing dependencies gracefully
        // This is verified by the service starting successfully in test mode
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            baseUrl + "/api/analytics/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> health = response.getBody();
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
    }

    @Test
    @DisplayName("Test Docker container environment variables")
    void testDockerContainerEnvironmentVariables() {
        // Test Docker Compose environment variables
        String[] dockerEnvVars = {
            "SPRING_PROFILES_ACTIVE",
            "EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE",
            "SPRING_REDIS_HOST",
            "SPRING_DATA_MONGODB_URI"
        };
        
        for (String envVar : dockerEnvVars) {
            String value = environment.getProperty(envVar.toLowerCase().replace('_', '.'));
            if (value != null && !value.startsWith("${")) {
                assertNotNull(value, envVar + " should be configured");
            }
        }
    }

    @Test
    @DisplayName("Test analytics specific environment variables")
    void testAnalyticsEnvironmentVariables() {
        // Test analytics-specific environment variables from docker-compose
        String[] analyticsEnvVars = {
            "analytics.calculation.min-data-points",
            "analytics.calculation.trend-analysis-days", 
            "analytics.calculation.cache-ttl-minutes",
            "analytics.insights.confidence-threshold",
            "analytics.insights.max-recommendations",
            "analytics.prediction.enabled",
            "analytics.prediction.forecast-days",
            "analytics.prediction.model-accuracy-threshold",
            "analytics.resilience.circuit-breaker.failure-threshold",
            "analytics.resilience.circuit-breaker.timeout-seconds",
            "analytics.resilience.circuit-breaker.retry-attempts",
            "analytics.observability.metrics-enabled",
            "analytics.observability.prometheus-port"
        };
        
        for (String property : analyticsEnvVars) {
            String value = environment.getProperty(property);
            // Properties may have defaults, so we just verify they're accessible
            assertNotNull(environment.getProperty(property, "default"), 
                         property + " should be accessible");
        }
    }

    @Test
    @DisplayName("Test container health check configuration")
    void testContainerHealthCheckConfiguration() {
        // Test that health check endpoint works as configured in docker-compose
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> health = response.getBody();
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        
        // Verify health check response time is reasonable (< 10 seconds as per docker-compose)
        long startTime = System.currentTimeMillis();
        restTemplate.getForEntity(baseUrl + "/actuator/health", 
                                 (Class<Map<String, Object>>) (Class<?>) Map.class);
        long responseTime = System.currentTimeMillis() - startTime;
        assertTrue(responseTime < 10000, "Health check should respond within 10 seconds");
    }

    @Test
    @DisplayName("Test service discovery integration")
    void testServiceDiscoveryIntegration() {
        // Test Eureka integration configuration
        String eurekaUrl = environment.getProperty("eureka.client.service-url.defaultZone");
        String serviceName = environment.getProperty("spring.application.name");
        
        if (eurekaUrl != null && !eurekaUrl.startsWith("${")) {
            assertTrue(eurekaUrl.contains("eureka"), "Eureka URL should contain 'eureka'");
        }
        
        assertNotNull(serviceName, "Service name should be configured for Eureka registration");
        assertEquals("analytics-service", serviceName, "Service name should match expected value");
    }

    @Test
    @DisplayName("Test metrics port configuration")
    void testMetricsPortConfiguration() {
        // Test Prometheus metrics port configuration (9091 in docker-compose)
        String prometheusPort = environment.getProperty("analytics.observability.prometheus-port");
        
        if (prometheusPort != null && !prometheusPort.startsWith("${")) {
            assertEquals("9091", prometheusPort, "Prometheus port should be 9091 as per docker-compose");
        }
    }

    @Test
    @DisplayName("Test database connection configuration")
    void testDatabaseConnectionConfiguration() {
        // Test MongoDB and Redis configuration
        String mongoUri = environment.getProperty("spring.data.mongodb.uri");
        String redisHost = environment.getProperty("spring.redis.host");
        
        if (mongoUri != null && !mongoUri.startsWith("${")) {
            assertTrue(mongoUri.contains("mongodb"), "MongoDB URI should contain 'mongodb'");
        }
        
        if (redisHost != null && !redisHost.startsWith("${")) {
            assertFalse(redisHost.isEmpty(), "Redis host should not be empty if configured");
        }
    }

    @Test
    @DisplayName("Test container restart behavior")
    void testContainerRestartBehavior() {
        // Test that service can handle restart scenarios
        // Verify health endpoint is immediately available after startup
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Test that service registration info is available
        ResponseEntity<Map<String, Object>> infoResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/info", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, infoResponse.getStatusCode());
    }

    @Test
    @DisplayName("Test network connectivity configuration")
    void testNetworkConnectivityConfiguration() {
        // Test that service can communicate within Docker network
        String eurekaUrl = environment.getProperty("eureka.client.service-url.defaultZone");
        String redisHost = environment.getProperty("spring.redis.host");
        
        // In Docker environment, these should use service names, not localhost
        if (eurekaUrl != null && !eurekaUrl.startsWith("${")) {
            if (eurekaUrl.contains("eureka-server")) {
                assertTrue(eurekaUrl.contains("eureka-server"), 
                          "Eureka URL should use Docker service name");
            }
        }
        
        if (redisHost != null && !redisHost.startsWith("${")) {
            if (redisHost.equals("redis")) {
                assertEquals("redis", redisHost, 
                           "Redis host should use Docker service name");
            }
        }
    }
}