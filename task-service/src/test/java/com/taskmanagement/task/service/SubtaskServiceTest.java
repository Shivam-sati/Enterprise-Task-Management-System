package com.taskmanagement.task.service;

import com.taskmanagement.task.dto.CreateSubtaskRequest;
import com.taskmanagement.task.dto.SubtaskResponse;
import com.taskmanagement.task.exception.SubtaskNotFoundException;
import com.taskmanagement.task.exception.TaskNotFoundException;
import com.taskmanagement.task.mapper.SubtaskMapper;
import com.taskmanagement.task.model.Subtask;
import com.taskmanagement.task.model.SubtaskStatus;
import com.taskmanagement.task.repository.SubtaskRepository;
import com.taskmanagement.task.repository.TaskRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubtaskServiceTest {
    
    @Mock
    private SubtaskRepository subtaskRepository;
    
    @Mock
    private TaskRepository taskRepository;
    
    @Mock
    private SubtaskMapper subtaskMapper;
    
    @InjectMocks
    private SubtaskService subtaskService;
    
    private Subtask testSubtask;
    private CreateSubtaskRequest createRequest;
    private SubtaskResponse subtaskResponse;
    
    @BeforeEach
    void setUp() {
        testSubtask = new Subtask("task123", "Test Subtask", 0);
        testSubtask.setSubtaskId("subtask123");
        
        createRequest = new CreateSubtaskRequest();
        createRequest.setTitle("Test Subtask");
        createRequest.setDescription("Test Description");
        
        subtaskResponse = new SubtaskResponse();
        subtaskResponse.setSubtaskId("subtask123");
        subtaskResponse.setTaskId("task123");
        subtaskResponse.setTitle("Test Subtask");
        subtaskResponse.setStatus(SubtaskStatus.TODO);
    }
    
    @Test
    void createSubtask_Success() {
        // Arrange
        when(taskRepository.existsByTaskIdAndUserId("task123", "user123")).thenReturn(true);
        when(subtaskRepository.findTopByTaskIdOrderByOrderDesc("task123")).thenReturn(Optional.empty());
        when(subtaskMapper.toEntity(createRequest, "task123", 0)).thenReturn(testSubtask);
        when(subtaskRepository.save(testSubtask)).thenReturn(testSubtask);
        when(subtaskMapper.toResponse(testSubtask)).thenReturn(subtaskResponse);
        
        // Act
        SubtaskResponse result = subtaskService.createSubtask("task123", "user123", createRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("subtask123", result.getSubtaskId());
        assertEquals("task123", result.getTaskId());
        verify(subtaskRepository).save(testSubtask);
    }
    
    @Test
    void createSubtask_TaskNotFound_ThrowsException() {
        // Arrange
        when(taskRepository.existsByTaskIdAndUserId("task123", "user123")).thenReturn(false);
        
        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> {
            subtaskService.createSubtask("task123", "user123", createRequest);
        });
        
        verify(subtaskRepository, never()).save(any());
    }
    
    @Test
    void createSubtask_WithCustomOrder_UsesProvidedOrder() {
        // Arrange
        createRequest.setOrder(5);
        when(taskRepository.existsByTaskIdAndUserId("task123", "user123")).thenReturn(true);
        when(subtaskMapper.toEntity(createRequest, "task123", 5)).thenReturn(testSubtask);
        when(subtaskRepository.save(testSubtask)).thenReturn(testSubtask);
        when(subtaskMapper.toResponse(testSubtask)).thenReturn(subtaskResponse);
        
        // Act
        SubtaskResponse result = subtaskService.createSubtask("task123", "user123", createRequest);
        
        // Assert
        assertNotNull(result);
        verify(subtaskMapper).toEntity(createRequest, "task123", 5);
    }
    
    @Test
    void getSubtasksByTaskId_Success() {
        // Arrange
        List<Subtask> subtasks = Arrays.asList(testSubtask);
        when(taskRepository.existsByTaskIdAndUserId("task123", "user123")).thenReturn(true);
        when(subtaskRepository.findByTaskIdOrderByOrder("task123")).thenReturn(subtasks);
        when(subtaskMapper.toResponse(testSubtask)).thenReturn(subtaskResponse);
        
        // Act
        List<SubtaskResponse> result = subtaskService.getSubtasksByTaskId("task123", "user123");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("subtask123", result.get(0).getSubtaskId());
    }
    
    @Test
    void getSubtasksByTaskId_TaskNotFound_ThrowsException() {
        // Arrange
        when(taskRepository.existsByTaskIdAndUserId("task123", "user123")).thenReturn(false);
        
        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> {
            subtaskService.getSubtasksByTaskId("task123", "user123");
        });
    }
    
    @Test
    void updateSubtaskStatus_Success() {
        // Arrange
        when(taskRepository.existsByTaskIdAndUserId("task123", "user123")).thenReturn(true);
        when(subtaskRepository.findBySubtaskId("subtask123")).thenReturn(Optional.of(testSubtask));
        when(subtaskRepository.save(testSubtask)).thenReturn(testSubtask);
        when(subtaskMapper.toResponse(testSubtask)).thenReturn(subtaskResponse);
        
        // Act
        SubtaskResponse result = subtaskService.updateSubtaskStatus("subtask123", "task123", "user123", SubtaskStatus.COMPLETED);
        
        // Assert
        assertNotNull(result);
        assertEquals(SubtaskStatus.COMPLETED, testSubtask.getStatus());
        verify(subtaskRepository).save(testSubtask);
    }
    
    @Test
    void updateSubtaskStatus_SubtaskNotFound_ThrowsException() {
        // Arrange
        when(taskRepository.existsByTaskIdAndUserId("task123", "user123")).thenReturn(true);
        when(subtaskRepository.findBySubtaskId("subtask123")).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(SubtaskNotFoundException.class, () -> {
            subtaskService.updateSubtaskStatus("subtask123", "task123", "user123", SubtaskStatus.COMPLETED);
        });
    }
    
    @Test
    void updateSubtaskStatus_SubtaskBelongsToDifferentTask_ThrowsException() {
        // Arrange
        testSubtask.setTaskId("different-task");
        when(taskRepository.existsByTaskIdAndUserId("task123", "user123")).thenReturn(true);
        when(subtaskRepository.findBySubtaskId("subtask123")).thenReturn(Optional.of(testSubtask));
        
        // Act & Assert
        assertThrows(SubtaskNotFoundException.class, () -> {
            subtaskService.updateSubtaskStatus("subtask123", "task123", "user123", SubtaskStatus.COMPLETED);
        });
    }
    
    @Test
    void deleteSubtask_Success() {
        // Arrange
        when(taskRepository.existsByTaskIdAndUserId("task123", "user123")).thenReturn(true);
        when(subtaskRepository.findBySubtaskId("subtask123")).thenReturn(Optional.of(testSubtask));
        when(subtaskRepository.findByTaskIdAndOrderGreaterThanEqual("task123", 0)).thenReturn(Arrays.asList());
        
        // Act
        subtaskService.deleteSubtask("subtask123", "task123", "user123");
        
        // Assert
        verify(subtaskRepository).delete(testSubtask);
    }
    
    @Test
    void areAllSubtasksCompleted_NoSubtasks_ReturnsTrue() {
        // Arrange
        when(subtaskRepository.countByTaskId("task123")).thenReturn(0L);
        
        // Act
        boolean result = subtaskService.areAllSubtasksCompleted("task123");
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void areAllSubtasksCompleted_AllCompleted_ReturnsTrue() {
        // Arrange
        when(subtaskRepository.countByTaskId("task123")).thenReturn(3L);
        when(subtaskRepository.countByTaskIdAndStatus("task123", SubtaskStatus.COMPLETED)).thenReturn(3L);
        
        // Act
        boolean result = subtaskService.areAllSubtasksCompleted("task123");
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void areAllSubtasksCompleted_SomeIncomplete_ReturnsFalse() {
        // Arrange
        when(subtaskRepository.countByTaskId("task123")).thenReturn(3L);
        when(subtaskRepository.countByTaskIdAndStatus("task123", SubtaskStatus.COMPLETED)).thenReturn(2L);
        
        // Act
        boolean result = subtaskService.areAllSubtasksCompleted("task123");
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void reorderSubtasks_Success() {
        // Arrange
        Subtask subtask1 = new Subtask("task123", "Subtask 1", 0);
        subtask1.setSubtaskId("sub1");
        Subtask subtask2 = new Subtask("task123", "Subtask 2", 1);
        subtask2.setSubtaskId("sub2");
        
        List<Subtask> subtasks = Arrays.asList(subtask1, subtask2);
        List<String> newOrder = Arrays.asList("sub2", "sub1");
        
        when(subtaskRepository.findByTaskIdOrderByOrder("task123")).thenReturn(subtasks);
        when(subtaskRepository.save(any(Subtask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subtaskMapper.toResponse(any(Subtask.class))).thenReturn(subtaskResponse);
        
        // Act
        List<SubtaskResponse> result = subtaskService.reorderSubtasks("task123", newOrder);
        
        // Assert
        assertNotNull(result);
        verify(subtaskRepository, times(2)).save(any(Subtask.class));
        assertEquals(0, subtask2.getOrder()); // sub2 should now be first
        assertEquals(1, subtask1.getOrder()); // sub1 should now be second
    }
    
    @Test
    void getCompletedSubtaskCount_Success() {
        // Arrange
        when(subtaskRepository.countByTaskIdAndStatus("task123", SubtaskStatus.COMPLETED)).thenReturn(5L);
        
        // Act
        long result = subtaskService.getCompletedSubtaskCount("task123");
        
        // Assert
        assertEquals(5L, result);
    }
    
    @Test
    void getTotalSubtaskCount_Success() {
        // Arrange
        when(subtaskRepository.countByTaskId("task123")).thenReturn(10L);
        
        // Act
        long result = subtaskService.getTotalSubtaskCount("task123");
        
        // Assert
        assertEquals(10L, result);
    }
}