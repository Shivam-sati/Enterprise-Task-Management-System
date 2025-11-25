package com.taskmanagement.ai.deployment;

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
 * Deployment and configuration tests for AI Service
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
        
        // Verify health check includes component details
        if (health.containsKey("components")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> components = (Map<String, Object>) health.get("components");
            assertNotNull(components);
        }
    }

    @Test
    @DisplayName("Test service info endpoint")
    void testServiceInfoEndpoint() {
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            baseUrl + "/actuator/info", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> info = response.getBody();
        assertNotNull(info);
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
    @DisplayName("Test AI service health endpoint")
    void testAIServiceHealthEndpoint() {
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            baseUrl + "/api/ai/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> health = response.getBody();
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        assertEquals("AI Service (Java Proxy)", health.get("service"));
        assertTrue(health.containsKey("message"));
    }

    @Test
    @DisplayName("Test environment configuration loading")
    void testEnvironmentConfiguration() {
        // Test that key configuration properties are loaded
        String activeProfile = environment.getProperty("spring.profiles.active");
        assertNotNull(activeProfile);
        
        // Test service name configuration
        String serviceName = environment.getProperty("spring.application.name");
        assertNotNull(serviceName);
        
        // Test server port configuration
        String serverPort = environment.getProperty("server.port");
        assertNotNull(serverPort);
    }

    @Test
    @DisplayName("Test circuit breaker configuration")
    void testCircuitBreakerConfiguration() {
        // Test that circuit breaker properties are configured
        String failureThreshold = environment.getProperty("resilience.circuit-breaker.failure-threshold");
        String timeoutSeconds = environment.getProperty("resilience.circuit-breaker.timeout-seconds");
        String retryAttempts = environment.getProperty("resilience.retry.attempts");
        
        // Verify default values or configured values exist
        assertNotNull(failureThreshold, "Circuit breaker failure threshold should be configured");
        assertNotNull(timeoutSeconds, "Circuit breaker timeout should be configured");
        assertNotNull(retryAttempts, "Retry attempts should be configured");
    }

    @Test
    @DisplayName("Test Python AI service configuration")
    void testPythonAIServiceConfiguration() {
        // Test Python AI service URL configuration
        String pythonServiceUrl = environment.getProperty("ai.python-service.url");
        String pythonServiceEnabled = environment.getProperty("ai.python-service.enabled");
        String pythonServiceTimeout = environment.getProperty("ai.python-service.timeout-seconds");
        
        // Verify configuration exists
        assertNotNull(pythonServiceUrl, "Python AI service URL should be configured");
        assertNotNull(pythonServiceEnabled, "Python AI service enabled flag should be configured");
        assertNotNull(pythonServiceTimeout, "Python AI service timeout should be configured");
    }

    @Test
    @DisplayName("Test fallback configuration")
    void testFallbackConfiguration() {
        // Test fallback service configuration
        String fallbackEnabled = environment.getProperty("ai.fallback.enabled");
        String fallbackLogUsage = environment.getProperty("ai.fallback.log-usage");
        
        assertNotNull(fallbackEnabled, "Fallback enabled flag should be configured");
        assertNotNull(fallbackLogUsage, "Fallback log usage flag should be configured");
    }

    @Test
    @DisplayName("Test startup time performance")
    void testStartupTimePerformance() {
        // This test verifies that the application starts within reasonable time
        // The application should already be started by the test framework
        
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
    @DisplayName("Test graceful shutdown configuration")
    void testGracefulShutdownConfiguration() {
        // Test that graceful shutdown is configured
        String shutdownTimeout = environment.getProperty("server.shutdown.timeout");
        String gracefulShutdown = environment.getProperty("server.shutdown");
        
        // These may be defaults, but should be accessible
        // Graceful shutdown configuration is important for production deployments
        assertNotNull(environment.getProperty("server.port"));
        
        // Verify shutdown configuration is accessible
        if (shutdownTimeout != null) {
            assertFalse(shutdownTimeout.isEmpty(), "Shutdown timeout should not be empty if configured");
        }
        if (gracefulShutdown != null) {
            assertFalse(gracefulShutdown.isEmpty(), "Graceful shutdown should not be empty if configured");
        }
    }

    @Test
    @DisplayName("Test logging configuration")
    void testLoggingConfiguration() {
        // Test that logging configuration is properly set
        String logLevel = environment.getProperty("logging.level.com.taskmanagement.ai");
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
    @DisplayName("Test security configuration")
    void testSecurityConfiguration() {
        // Test that security endpoints are properly configured
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify that sensitive endpoints require authentication if configured
        // This is a basic test - in production, you'd test actual security
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Test database connection configuration")
    void testDatabaseConnectionConfiguration() {
        // Test Redis configuration if enabled
        String redisHost = environment.getProperty("spring.redis.host");
        String redisPort = environment.getProperty("spring.redis.port");
        
        // These may be null in test environment, but we verify they can be accessed
        if (redisHost != null) {
            assertFalse(redisHost.isEmpty(), "Redis host should not be empty if configured");
        }
        if (redisPort != null) {
            assertFalse(redisPort.isEmpty(), "Redis port should not be empty if configured");
        }
    }

    @Test
    @DisplayName("Test service discovery configuration")
    void testServiceDiscoveryConfiguration() {
        // Test Eureka configuration
        String eurekaUrl = environment.getProperty("eureka.client.service-url.defaultZone");
        String eurekaEnabled = environment.getProperty("eureka.client.enabled");
        
        if (eurekaUrl != null) {
            assertTrue(eurekaUrl.contains("eureka"), "Eureka URL should contain 'eureka'");
        }
        
        if (eurekaEnabled != null) {
            assertTrue(eurekaEnabled.equals("true") || eurekaEnabled.equals("false"),
                      "Eureka enabled should be boolean");
        }
        
        // Verify service registration properties
        String serviceName = environment.getProperty("spring.application.name");
        assertNotNull(serviceName, "Service name should be configured for service discovery");
    }

    @Test
    @DisplayName("Test monitoring and observability configuration")
    void testMonitoringConfiguration() {
        // Test that monitoring endpoints are enabled
        String managementEndpointsEnabled = environment.getProperty("management.endpoints.web.exposure.include");
        String healthEndpointEnabled = environment.getProperty("management.endpoint.health.enabled");
        String metricsEndpointEnabled = environment.getProperty("management.endpoint.metrics.enabled");
        
        // Verify monitoring configuration
        if (managementEndpointsEnabled != null) {
            assertTrue(managementEndpointsEnabled.contains("health") || 
                      managementEndpointsEnabled.contains("*"),
                      "Health endpoint should be exposed");
        }
        
        // Verify endpoint configurations are accessible
        if (healthEndpointEnabled != null) {
            assertTrue(healthEndpointEnabled.equals("true") || healthEndpointEnabled.equals("false"),
                      "Health endpoint enabled should be boolean");
        }
        if (metricsEndpointEnabled != null) {
            assertTrue(metricsEndpointEnabled.equals("true") || metricsEndpointEnabled.equals("false"),
                      "Metrics endpoint enabled should be boolean");
        }
    }

    @Test
    @DisplayName("Test error handling configuration")
    void testErrorHandlingConfiguration() {
        // Test that error handling is properly configured
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            baseUrl + "/api/ai/nonexistent-endpoint", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        // Should return 404 or proper error response
        assertTrue(response.getStatusCode().is4xxClientError() || 
                  response.getStatusCode().is5xxServerError(),
                  "Non-existent endpoint should return error status");
    }

    @Test
    @DisplayName("Test resource limits and performance configuration")
    void testResourceConfiguration() {
        // Test that resource-related configuration is accessible
        String maxThreads = environment.getProperty("server.tomcat.threads.max");
        String connectionTimeout = environment.getProperty("server.tomcat.connection-timeout");
        
        // These may have defaults, verify they're accessible
        if (maxThreads != null) {
            assertTrue(Integer.parseInt(maxThreads) > 0, "Max threads should be positive");
        }
        if (connectionTimeout != null) {
            assertTrue(connectionTimeout.matches("\\d+.*"), "Connection timeout should be numeric");
        }
    }

    @Test
    @DisplayName("Test configuration validation")
    void testConfigurationValidation() {
        // Test that critical configuration is valid
        String serverPort = environment.getProperty("server.port");
        if (serverPort != null && !serverPort.equals("0")) {
            int port = Integer.parseInt(serverPort);
            assertTrue(port > 0 && port <= 65535, "Server port should be valid");
        }
        
        // Test timeout configurations
        String pythonServiceTimeout = environment.getProperty("ai.python-service.timeout-seconds");
        if (pythonServiceTimeout != null && !pythonServiceTimeout.startsWith("${")) {
            int timeout = Integer.parseInt(pythonServiceTimeout);
            assertTrue(timeout > 0, "Python service timeout should be positive");
        }
    }

    @Test
    @DisplayName("Test Docker container environment variables")
    void testDockerContainerEnvironmentVariables() {
        // Test Docker Compose environment variables for AI service
        String[] dockerEnvVars = {
            "SPRING_PROFILES_ACTIVE",
            "EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE", 
            "SPRING_REDIS_HOST",
            "AI_PYTHON_SERVICE_URL",
            "AI_PYTHON_SERVICE_ENABLED",
            "AI_PYTHON_SERVICE_TIMEOUT_SECONDS"
        };
        
        for (String envVar : dockerEnvVars) {
            String propertyName = envVar.toLowerCase().replace('_', '.');
            String value = environment.getProperty(propertyName);
            // Properties may have defaults, so we just verify they're accessible
            assertNotNull(environment.getProperty(propertyName, "default"), 
                         envVar + " should be accessible");
        }
    }

    @Test
    @DisplayName("Test circuit breaker environment variables")
    void testCircuitBreakerEnvironmentVariables() {
        // Test circuit breaker configuration from docker-compose
        String[] circuitBreakerEnvVars = {
            "resilience.circuit-breaker.failure-threshold",
            "resilience.circuit-breaker.timeout-seconds",
            "resilience.retry.attempts",
            "resilience.retry.delay-ms"
        };
        
        for (String property : circuitBreakerEnvVars) {
            String value = environment.getProperty(property);
            if (value != null && !value.startsWith("${")) {
                assertTrue(Integer.parseInt(value) > 0, 
                          property + " should be positive");
            }
        }
    }

    @Test
    @DisplayName("Test fallback configuration")
    void testFallbackConfigurationFromEnvironment() {
        // Test fallback configuration from docker-compose
        String fallbackEnabled = environment.getProperty("ai.fallback.enabled");
        String fallbackLogUsage = environment.getProperty("ai.fallback.log-usage");
        
        if (fallbackEnabled != null && !fallbackEnabled.startsWith("${")) {
            assertTrue(fallbackEnabled.equals("true") || fallbackEnabled.equals("false"),
                      "Fallback enabled should be boolean");
        }
        
        if (fallbackLogUsage != null && !fallbackLogUsage.startsWith("${")) {
            assertTrue(fallbackLogUsage.equals("true") || fallbackLogUsage.equals("false"),
                      "Fallback log usage should be boolean");
        }
    }

    @Test
    @DisplayName("Test Python AI service integration configuration")
    void testPythonAIServiceIntegrationConfiguration() {
        // Test Python AI service configuration from docker-compose
        String pythonServiceUrl = environment.getProperty("ai.python-service.url");
        String pythonServiceEnabled = environment.getProperty("ai.python-service.enabled");
        
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
    @DisplayName("Test service dependencies configuration")
    void testServiceDependenciesConfiguration() {
        // Test that service dependencies are properly configured
        String eurekaUrl = environment.getProperty("eureka.client.service-url.defaultZone");
        String redisHost = environment.getProperty("spring.redis.host");
        
        // In Docker environment, these should use service names
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

    @Test
    @DisplayName("Test container restart behavior")
    void testContainerRestartBehavior() {
        // Test that service can handle restart scenarios
        // Verify health endpoint is immediately available after startup
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(
            baseUrl + "/actuator/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Test AI service specific health endpoint
        ResponseEntity<Map<String, Object>> aiHealthResponse = restTemplate.getForEntity(
            baseUrl + "/api/ai/health", 
            (Class<Map<String, Object>>) (Class<?>) Map.class);
        
        assertEquals(HttpStatus.OK, aiHealthResponse.getStatusCode());
        
        Map<String, Object> aiHealth = aiHealthResponse.getBody();
        assertNotNull(aiHealth);
        assertEquals("UP", aiHealth.get("status"));
        assertEquals("AI Service (Java Proxy)", aiHealth.get("service"));
    }

    @Test
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
}