package com.taskmanagement.task.service;

import com.taskmanagement.task.dto.CreateTaskRequest;
import com.taskmanagement.task.dto.TaskResponse;
import com.taskmanagement.task.dto.UpdateTaskRequest;
import com.taskmanagement.task.exception.TaskNotFoundException;
import com.taskmanagement.task.mapper.TaskMapper;
import com.taskmanagement.task.model.Task;
import com.taskmanagement.task.model.TaskPriority;
import com.taskmanagement.task.model.TaskStatus;
import com.taskmanagement.task.repository.SubtaskRepository;
import com.taskmanagement.task.repository.TaskRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    
    @Mock
    private TaskRepository taskRepository;
    
    @Mock
    private SubtaskRepository subtaskRepository;
    
    @Mock
    private SubtaskService subtaskService;
    
    @Mock
    private TagService tagService;
    
    @Mock
    private DependencyService dependencyService;
    
    @Mock
    private RecurringTaskService recurringTaskService;
    
    @Mock
    private TaskMapper taskMapper;
    
    @InjectMocks
    private TaskService taskService;
    
    private Task testTask;
    private CreateTaskRequest createRequest;
    private UpdateTaskRequest updateRequest;
    private TaskResponse taskResponse;
    
    @BeforeEach
    void setUp() {
        testTask = new Task("user123", "Test Task", "Test Description");
        testTask.setTaskId("task123");
        testTask.setPriority(TaskPriority.HIGH);
        testTask.setTags(Arrays.asList("work", "urgent"));
        
        createRequest = new CreateTaskRequest();
        createRequest.setTitle("Test Task");
        createRequest.setDescription("Test Description");
        createRequest.setPriority(TaskPriority.HIGH);
        createRequest.setTags(Arrays.asList("work", "urgent"));
        
        updateRequest = new UpdateTaskRequest();
        updateRequest.setTitle("Updated Task");
        updateRequest.setStatus(TaskStatus.IN_PROGRESS);
        
        taskResponse = new TaskResponse();
        taskResponse.setTaskId("task123");
        taskResponse.setTitle("Test Task");
        taskResponse.setDescription("Test Description");
        taskResponse.setPriority(TaskPriority.HIGH);
        taskResponse.setStatus(TaskStatus.TODO);
    }
    
    @Test
    void createTask_Success() {
        // Arrange
        when(taskMapper.toEntity(createRequest, "user123")).thenReturn(testTask);
        when(taskRepository.save(testTask)).thenReturn(testTask);
        when(taskMapper.toResponse(testTask)).thenReturn(taskResponse);
        
        // Act
        TaskResponse result = taskService.createTask("user123", createRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("task123", result.getTaskId());
        assertEquals("Test Task", result.getTitle());
        
        verify(dependencyService).validateAllDependencies(testTask.getTaskId(), testTask.getDependencies());
        verify(tagService).updateTagUsageCounts("user123", null, createRequest.getTags());
        verify(taskRepository).save(testTask);
    }
    
    @Test
    void createTask_WithDependencies_ValidatesDependencies() {
        // Arrange
        createRequest.setDependencies(Arrays.asList("dep1", "dep2"));
        when(taskRepository.existsByTaskIdAndUserId("dep1", "user123")).thenReturn(true);
        when(taskRepository.existsByTaskIdAndUserId("dep2", "user123")).thenReturn(true);
        when(taskMapper.toEntity(createRequest, "user123")).thenReturn(testTask);
        when(taskRepository.save(testTask)).thenReturn(testTask);
        when(taskMapper.toResponse(testTask)).thenReturn(taskResponse);
        
        // Act
        TaskResponse result = taskService.createTask("user123", createRequest);
        
        // Assert
        assertNotNull(result);
        verify(taskRepository).existsByTaskIdAndUserId("dep1", "user123");
        verify(taskRepository).existsByTaskIdAndUserId("dep2", "user123");
    }
    
    @Test
    void createTask_WithInvalidDependency_ThrowsException() {
        // Arrange
        createRequest.setDependencies(Arrays.asList("invalid-dep"));
        when(taskRepository.existsByTaskIdAndUserId("invalid-dep", "user123")).thenReturn(false);
        
        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> {
            taskService.createTask("user123", createRequest);
        });
        
        verify(taskRepository, never()).save(any());
    }
    
    @Test
    void getTaskById_Success() {
        // Arrange
        when(taskRepository.findByTaskIdAndUserId("task123", "user123")).thenReturn(Optional.of(testTask));
        when(subtaskRepository.findByTaskIdOrderByOrder("task123")).thenReturn(Arrays.asList());
        when(taskMapper.toResponse(testTask, Arrays.asList())).thenReturn(taskResponse);
        
        // Act
        TaskResponse result = taskService.getTaskById("task123", "user123");
        
        // Assert
        assertNotNull(result);
        assertEquals("task123", result.getTaskId());
        verify(taskRepository).findByTaskIdAndUserId("task123", "user123");
    }
    
    @Test
    void getTaskById_NotFound_ThrowsException() {
        // Arrange
        when(taskRepository.findByTaskIdAndUserId("task123", "user123")).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> {
            taskService.getTaskById("task123", "user123");
        });
    }
    
    @Test
    void updateTask_Success() {
        // Arrange
        when(taskRepository.findByTaskIdAndUserId("task123", "user123")).thenReturn(Optional.of(testTask));
        when(taskRepository.save(testTask)).thenReturn(testTask);
        when(subtaskRepository.findByTaskIdOrderByOrder("task123")).thenReturn(Arrays.asList());
        when(taskMapper.toResponse(testTask, Arrays.asList())).thenReturn(taskResponse);
        
        // Act
        TaskResponse result = taskService.updateTask("task123", "user123", updateRequest);
        
        // Assert
        assertNotNull(result);
        verify(taskMapper).updateEntity(testTask, updateRequest);
        verify(taskRepository).save(testTask);
    }
    
    @Test
    void updateTaskStatus_Success() {
        // Arrange
        when(taskRepository.findByTaskIdAndUserId("task123", "user123")).thenReturn(Optional.of(testTask));
        when(dependencyService.canTaskBeCompleted("task123", "user123")).thenReturn(true);
        when(taskRepository.save(testTask)).thenReturn(testTask);
        when(subtaskRepository.findByTaskIdOrderByOrder("task123")).thenReturn(Arrays.asList());
        when(taskMapper.toResponse(testTask, Arrays.asList())).thenReturn(taskResponse);
        
        // Act
        TaskResponse result = taskService.updateTaskStatus("task123", "user123", TaskStatus.COMPLETED);
        
        // Assert
        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETED, testTask.getStatus());
        verify(taskRepository).save(testTask);
    }
    
    @Test
    void updateTaskStatus_CompletedRecurringTask_CreatesNextOccurrence() {
        // Arrange
        testTask.setRecurring(true);
        when(taskRepository.findByTaskIdAndUserId("task123", "user123")).thenReturn(Optional.of(testTask));
        when(dependencyService.canTaskBeCompleted("task123", "user123")).thenReturn(true);
        when(taskRepository.save(testTask)).thenReturn(testTask);
        when(subtaskRepository.findByTaskIdOrderByOrder("task123")).thenReturn(Arrays.asList());
        when(taskMapper.toResponse(testTask, Arrays.asList())).thenReturn(taskResponse);
        
        // Act
        TaskResponse result = taskService.updateTaskStatus("task123", "user123", TaskStatus.COMPLETED);
        
        // Assert
        assertNotNull(result);
        verify(recurringTaskService).createNextOccurrence(testTask);
    }
    
    @Test
    void updateTaskStatus_DependenciesNotMet_ThrowsException() {
        // Arrange
        when(taskRepository.findByTaskIdAndUserId("task123", "user123")).thenReturn(Optional.of(testTask));
        when(dependencyService.canTaskBeCompleted("task123", "user123")).thenReturn(false);
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            taskService.updateTaskStatus("task123", "user123", TaskStatus.COMPLETED);
        });
        
        verify(taskRepository, never()).save(any());
    }
    
    @Test
    void deleteTask_Success() {
        // Arrange
        when(taskRepository.findByTaskIdAndUserId("task123", "user123")).thenReturn(Optional.of(testTask));
        
        // Act
        taskService.deleteTask("task123", "user123");
        
        // Assert
        verify(tagService).updateTagUsageCounts("user123", testTask.getTags(), null);
        verify(dependencyService).removeDependencyReferences("task123");
        verify(subtaskService).deleteAllSubtasksByTaskId("task123");
        verify(taskRepository).delete(testTask);
    }
    
    @Test
    void addDependency_Success() {
        // Arrange
        when(taskRepository.findByTaskIdAndUserId("task123", "user123")).thenReturn(Optional.of(testTask));
        when(taskRepository.existsByTaskIdAndUserId("dep123", "user123")).thenReturn(true);
        when(taskRepository.save(testTask)).thenReturn(testTask);
        when(subtaskRepository.findByTaskIdOrderByOrder("task123")).thenReturn(Arrays.asList());
        when(taskMapper.toResponse(testTask, Arrays.asList())).thenReturn(taskResponse);
        
        // Act
        TaskResponse result = taskService.addDependency("task123", "user123", "dep123");
        
        // Assert
        assertNotNull(result);
        verify(dependencyService).validateDependency("task123", "dep123");
        verify(taskRepository).save(testTask);
    }
    
    @Test
    void getOverdueTasks_Success() {
        // Arrange
        List<Task> overdueTasks = Arrays.asList(testTask);
        when(taskRepository.findOverdueTasks(eq("user123"), any(LocalDateTime.class))).thenReturn(overdueTasks);
        when(subtaskRepository.findByTaskIdOrderByOrder("task123")).thenReturn(Arrays.asList());
        when(taskMapper.toResponse(testTask, Arrays.asList())).thenReturn(taskResponse);
        
        // Act
        List<TaskResponse> result = taskService.getOverdueTasks("user123");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("task123", result.get(0).getTaskId());
    }
    
    @Test
    void getTaskStatistics_Success() {
        // Arrange
        when(taskRepository.findByUserId("user123")).thenReturn(Arrays.asList(testTask));
        when(taskRepository.countByUserIdAndStatus("user123", TaskStatus.TODO)).thenReturn(5L);
        when(taskRepository.countByUserIdAndStatus("user123", TaskStatus.IN_PROGRESS)).thenReturn(3L);
        when(taskRepository.countByUserIdAndStatus("user123", TaskStatus.COMPLETED)).thenReturn(10L);
        when(taskRepository.countByUserIdAndStatus("user123", TaskStatus.CANCELLED)).thenReturn(1L);
        when(taskRepository.countByUserIdAndPriority("user123", TaskPriority.HIGH)).thenReturn(2L);
        when(taskRepository.countByUserIdAndPriority("user123", TaskPriority.CRITICAL)).thenReturn(1L);
        when(taskRepository.findOverdueTasks(eq("user123"), any(LocalDateTime.class))).thenReturn(Arrays.asList());
        when(taskRepository.findByUserIdAndIsRecurringTrue("user123")).thenReturn(Arrays.asList());
        
        // Act
        var result = taskService.getTaskStatistics("user123");
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.get("total"));
        assertEquals(5L, result.get("todo"));
        assertEquals(3L, result.get("inProgress"));
        assertEquals(10L, result.get("completed"));
        assertEquals(1L, result.get("cancelled"));
        assertEquals(2L, result.get("highPriority"));
        assertEquals(1L, result.get("criticalPriority"));
        assertEquals(0L, result.get("overdue"));
        assertEquals(0L, result.get("recurring"));
    }
}