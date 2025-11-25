package com.taskmanagement.task.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.task.TaskServiceApplication;
import com.taskmanagement.task.dto.CreateTaskRequest;
import com.taskmanagement.task.dto.TaskResponse;
import com.taskmanagement.task.dto.UpdateTaskRequest;
import com.taskmanagement.task.model.TaskPriority;
import com.taskmanagement.task.model.TaskStatus;
import com.taskmanagement.task.repository.TaskRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TaskServiceApplication.class)
@Testcontainers
@AutoConfigureWebMvc
class TaskServiceIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        // Clean up database before each test
        taskRepository.deleteAll();
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void createTask_Success() throws Exception {
        // Arrange
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Integration Test Task");
        request.setDescription("This is a test task for integration testing");
        request.setPriority(TaskPriority.HIGH);
        request.setTags(Arrays.asList("test", "integration"));
        request.setDueDate(LocalDateTime.now().plusDays(7));
        
        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration Test Task"))
                .andExpect(jsonPath("$.description").value("This is a test task for integration testing"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.tags[0]").value("test"))
                .andExpect(jsonPath("$.tags[1]").value("integration"))
                .andExpect(jsonPath("$.taskId").exists());
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void createTask_InvalidData_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateTaskRequest request = new CreateTaskRequest();
        // Missing required title
        request.setDescription("Task without title");
        
        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void getTask_Success() throws Exception {
        // Arrange - Create a task first
        CreateTaskRequest createRequest = new CreateTaskRequest();
        createRequest.setTitle("Test Task");
        createRequest.setDescription("Test Description");
        createRequest.setPriority(TaskPriority.MEDIUM);
        
        String createResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        TaskResponse createdTask = objectMapper.readValue(createResponse, TaskResponse.class);
        
        // Act & Assert
        mockMvc.perform(get("/api/tasks/" + createdTask.getTaskId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(createdTask.getTaskId()))
                .andExpect(jsonPath("$.title").value("Test Task"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void getTask_NotFound_ReturnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/tasks/nonexistent-task-id"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void updateTask_Success() throws Exception {
        // Arrange - Create a task first
        CreateTaskRequest createRequest = new CreateTaskRequest();
        createRequest.setTitle("Original Title");
        createRequest.setDescription("Original Description");
        
        String createResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        TaskResponse createdTask = objectMapper.readValue(createResponse, TaskResponse.class);
        
        // Prepare update request
        UpdateTaskRequest updateRequest = new UpdateTaskRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setDescription("Updated Description");
        updateRequest.setStatus(TaskStatus.IN_PROGRESS);
        updateRequest.setPriority(TaskPriority.HIGH);
        
        // Act & Assert
        mockMvc.perform(put("/api/tasks/" + createdTask.getTaskId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void updateTaskStatus_Success() throws Exception {
        // Arrange - Create a task first
        CreateTaskRequest createRequest = new CreateTaskRequest();
        createRequest.setTitle("Task to Complete");
        createRequest.setDescription("This task will be completed");
        
        String createResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        TaskResponse createdTask = objectMapper.readValue(createResponse, TaskResponse.class);
        
        // Act & Assert
        mockMvc.perform(patch("/api/tasks/" + createdTask.getTaskId() + "/status")
                .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists());
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void deleteTask_Success() throws Exception {
        // Arrange - Create a task first
        CreateTaskRequest createRequest = new CreateTaskRequest();
        createRequest.setTitle("Task to Delete");
        createRequest.setDescription("This task will be deleted");
        
        String createResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        TaskResponse createdTask = objectMapper.readValue(createResponse, TaskResponse.class);
        
        // Act & Assert - Delete the task
        mockMvc.perform(delete("/api/tasks/" + createdTask.getTaskId()))
                .andExpect(status().isNoContent());
        
        // Verify task is deleted
        mockMvc.perform(get("/api/tasks/" + createdTask.getTaskId()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void getTasks_Pagination_Success() throws Exception {
        // Arrange - Create multiple tasks
        for (int i = 1; i <= 5; i++) {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTitle("Task " + i);
            request.setDescription("Description " + i);
            
            mockMvc.perform(post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
        
        // Act & Assert - Get first page
        mockMvc.perform(get("/api/tasks")
                .param("page", "0")
                .param("size", "3")
                .param("sortBy", "createdAt")
                .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void searchTasks_Success() throws Exception {
        // Arrange - Create tasks with different titles
        CreateTaskRequest request1 = new CreateTaskRequest();
        request1.setTitle("Important Meeting");
        request1.setDescription("Discuss project requirements");
        
        CreateTaskRequest request2 = new CreateTaskRequest();
        request2.setTitle("Code Review");
        request2.setDescription("Review pull requests");
        
        CreateTaskRequest request3 = new CreateTaskRequest();
        request3.setTitle("Important Documentation");
        request3.setDescription("Write API documentation");
        
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());
        
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());
        
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated());
        
        // Act & Assert - Search for "Important"
        mockMvc.perform(get("/api/tasks/search")
                .param("q", "Important"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void getTaskStatistics_Success() throws Exception {
        // Arrange - Create tasks with different statuses and priorities
        CreateTaskRequest todoTask = new CreateTaskRequest();
        todoTask.setTitle("TODO Task");
        todoTask.setPriority(TaskPriority.HIGH);
        
        CreateTaskRequest inProgressTask = new CreateTaskRequest();
        inProgressTask.setTitle("In Progress Task");
        inProgressTask.setPriority(TaskPriority.CRITICAL);
        
        // Create tasks
        String todoResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(todoTask)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String inProgressResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inProgressTask)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        TaskResponse inProgressTaskObj = objectMapper.readValue(inProgressResponse, TaskResponse.class);
        
        // Update one task to IN_PROGRESS
        mockMvc.perform(patch("/api/tasks/" + inProgressTaskObj.getTaskId() + "/status")
                .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk());
        
        // Act & Assert
        mockMvc.perform(get("/api/tasks/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.todo").value(1))
                .andExpect(jsonPath("$.inProgress").value(1))
                .andExpect(jsonPath("$.completed").value(0))
                .andExpect(jsonPath("$.highPriority").value(1))
                .andExpect(jsonPath("$.criticalPriority").value(1));
    }
}