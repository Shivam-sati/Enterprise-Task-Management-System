package com.taskmanagement.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.ai.service.PythonAIServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AIController.class)
class AIControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PythonAIServiceClient pythonAIServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void health_ShouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/ai/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("AI Service (Java Proxy)"))
            .andExpect(jsonPath("$.message").value("AI Service proxy is running successfully"));
    }

    @Test
    void parseTask_WhenValidRequest_ShouldReturnParsedTask() throws Exception {
        // Arrange
        Map<String, String> request = Map.of("text", "Create a new feature for user authentication");
        Map<String, Object> mockResponse = Map.of(
            "title", "Create a new feature for user authentication",
            "description", "Create a new feature for user authentication",
            "priority", "HIGH",
            "estimatedHours", 8,
            "tags", new String[]{"authentication", "feature"},
            "confidence", 0.9
        );

        when(pythonAIServiceClient.parseTask(any())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/ai/parse-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Create a new feature for user authentication"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.estimatedHours").value(8))
            .andExpect(jsonPath("$.confidence").value(0.9));
    }

    @Test
    void parseTask_WhenServiceThrowsException_ShouldReturnError() throws Exception {
        // Arrange
        Map<String, String> request = Map.of("text", "Test task");
        when(pythonAIServiceClient.parseTask(any())).thenThrow(new RuntimeException("Service unavailable"));

        // Act & Assert
        mockMvc.perform(post("/api/ai/parse-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Unable to process task parsing request"));
    }

    @Test
    void prioritizeTasks_WhenValidRequest_ShouldReturnPrioritizedTasks() throws Exception {
        // Arrange
        Map<String, Object> request = Map.of("tasks", List.of("task1", "task2", "task3"));
        Map<String, Object> mockResponse = Map.of(
            "prioritizedTasks", List.of("task2", "task1", "task3"),
            "reasoning", "Tasks prioritized based on urgency and dependencies",
            "confidence", 0.85
        );

        when(pythonAIServiceClient.prioritizeTasks(any())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/ai/prioritize-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.prioritizedTasks").isArray())
            .andExpect(jsonPath("$.reasoning").value("Tasks prioritized based on urgency and dependencies"))
            .andExpect(jsonPath("$.confidence").value(0.85));
    }

    @Test
    void getProductivityInsights_WhenValidRequest_ShouldReturnInsights() throws Exception {
        // Arrange
        Map<String, Object> mockResponse = Map.of(
            "insights", List.of(
                "You're most productive in the morning",
                "Consider breaking down large tasks"
            ),
            "recommendations", List.of(
                "Schedule important tasks before 11 AM",
                "Use time-blocking techniques"
            ),
            "confidence", 0.8
        );

        when(pythonAIServiceClient.getProductivityInsights()).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/ai/insights"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.insights").isArray())
            .andExpect(jsonPath("$.recommendations").isArray())
            .andExpect(jsonPath("$.confidence").value(0.8));
    }

    @Test
    void parseTask_WhenFallbackActivated_ShouldReturnFallbackResponse() throws Exception {
        // Arrange
        Map<String, String> request = Map.of("text", "Test task");
        Map<String, Object> fallbackResponse = Map.of(
            "title", "Test task",
            "description", "Test task",
            "priority", "MEDIUM",
            "estimatedHours", 2,
            "tags", new String[]{"fallback-parsed"},
            "confidence", 0.5,
            "source", "fallback",
            "message", "Response generated by fallback service - Python AI service unavailable"
        );

        when(pythonAIServiceClient.parseTask(any())).thenReturn(fallbackResponse);

        // Act & Assert
        mockMvc.perform(post("/api/ai/parse-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.source").value("fallback"))
            .andExpect(jsonPath("$.confidence").value(0.5))
            .andExpect(jsonPath("$.message").exists());
    }
}