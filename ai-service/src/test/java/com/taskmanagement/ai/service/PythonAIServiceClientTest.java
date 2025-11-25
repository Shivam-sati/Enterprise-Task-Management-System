package com.taskmanagement.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PythonAIServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DiscoveryClient discoveryClient;

    @Mock
    private FallbackService fallbackService;

    @Mock
    private ServiceInstance serviceInstance;

    private PythonAIServiceClient pythonAIServiceClient;

    @BeforeEach
    void setUp() {
        pythonAIServiceClient = new PythonAIServiceClient(restTemplate, discoveryClient, fallbackService);
        ReflectionTestUtils.setField(pythonAIServiceClient, "pythonServiceName", "ai-service-python");
    }

    @Test
    void parseTask_WhenPythonServiceAvailable_ShouldReturnResponse() {
        // Arrange
        Map<String, String> request = Map.of("text", "Test task");
        Map<String, Object> expectedResponse = Map.of("title", "Test task", "priority", "HIGH");
        
        when(serviceInstance.getHost()).thenReturn("localhost");
        when(serviceInstance.getPort()).thenReturn(8087);
        when(discoveryClient.getInstances("ai-service-python")).thenReturn(List.of(serviceInstance));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // Act
        Map<String, Object> result = pythonAIServiceClient.parseTask(request);

        // Assert
        assertEquals(expectedResponse, result);
        verify(restTemplate).exchange(
            eq("http://localhost:8087/api/ai/parse-task"),
            eq(HttpMethod.POST),
            any(),
            eq(Map.class)
        );
    }

    @Test
    void parseTask_WhenPythonServiceUnavailable_ShouldUseFallback() {
        // Arrange
        Map<String, String> request = Map.of("text", "Test task");
        Map<String, Object> fallbackResponse = Map.of("title", "Test task (Fallback)", "source", "fallback");
        
        when(discoveryClient.getInstances("ai-service-python")).thenReturn(Collections.emptyList());
        when(fallbackService.parseTaskFallback(request)).thenReturn(fallbackResponse);

        // Act
        Map<String, Object> result = pythonAIServiceClient.parseTask(request);

        // Assert
        assertEquals(fallbackResponse, result);
        verify(fallbackService).parseTaskFallback(request);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void parseTask_WhenRestTemplateThrowsException_ShouldUseFallback() {
        // Arrange
        Map<String, String> request = Map.of("text", "Test task");
        Map<String, Object> fallbackResponse = Map.of("title", "Test task (Fallback)", "source", "fallback");
        
        when(serviceInstance.getHost()).thenReturn("localhost");
        when(serviceInstance.getPort()).thenReturn(8087);
        when(discoveryClient.getInstances("ai-service-python")).thenReturn(List.of(serviceInstance));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
            .thenThrow(new RestClientException("Connection failed"));
        when(fallbackService.parseTaskFallback(eq(request))).thenReturn(fallbackResponse);

        // Act & Assert
        assertThrows(RestClientException.class, () -> pythonAIServiceClient.parseTask(request));
    }

    @Test
    void prioritizeTasks_WhenPythonServiceAvailable_ShouldReturnResponse() {
        // Arrange
        Map<String, Object> request = Map.of("tasks", List.of("task1", "task2"));
        Map<String, Object> expectedResponse = Map.of("prioritizedTasks", List.of("task2", "task1"));
        
        when(serviceInstance.getHost()).thenReturn("localhost");
        when(serviceInstance.getPort()).thenReturn(8087);
        when(discoveryClient.getInstances("ai-service-python")).thenReturn(List.of(serviceInstance));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // Act
        Map<String, Object> result = pythonAIServiceClient.prioritizeTasks(request);

        // Assert
        assertEquals(expectedResponse, result);
        verify(restTemplate).exchange(
            eq("http://localhost:8087/api/ai/prioritize-tasks"),
            eq(HttpMethod.POST),
            any(),
            eq(Map.class)
        );
    }

    @Test
    void getProductivityInsights_WhenPythonServiceAvailable_ShouldReturnResponse() {
        // Arrange
        Map<String, Object> expectedResponse = Map.of("insights", List.of("insight1", "insight2"));
        
        when(serviceInstance.getHost()).thenReturn("localhost");
        when(serviceInstance.getPort()).thenReturn(8087);
        when(discoveryClient.getInstances("ai-service-python")).thenReturn(List.of(serviceInstance));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // Act
        Map<String, Object> result = pythonAIServiceClient.getProductivityInsights();

        // Assert
        assertEquals(expectedResponse, result);
        verify(restTemplate).exchange(
            eq("http://localhost:8087/api/ai/insights"),
            eq(HttpMethod.GET),
            isNull(),
            eq(Map.class)
        );
    }

    @Test
    void getProductivityInsights_WhenPythonServiceUnavailable_ShouldUseFallback() {
        // Arrange
        Map<String, Object> fallbackResponse = Map.of("insights", List.of("fallback insight"), "source", "fallback");
        
        when(discoveryClient.getInstances("ai-service-python")).thenReturn(Collections.emptyList());
        when(fallbackService.getInsightsFallback()).thenReturn(fallbackResponse);

        // Act
        Map<String, Object> result = pythonAIServiceClient.getProductivityInsights();

        // Assert
        assertEquals(fallbackResponse, result);
        verify(fallbackService).getInsightsFallback();
        verifyNoInteractions(restTemplate);
    }
}