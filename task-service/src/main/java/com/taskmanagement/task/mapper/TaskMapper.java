package com.taskmanagement.task.mapper;

import com.taskmanagement.task.dto.*;
import com.taskmanagement.task.model.Task;
import com.taskmanagement.task.model.Subtask;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TaskMapper {
    
    public Task toEntity(CreateTaskRequest request, String userId) {
        Task task = new Task(userId, request.getTitle(), request.getDescription());
        
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        
        if (request.getTags() != null) {
            task.setTags(request.getTags());
        }
        
        task.setDueDate(request.getDueDate());
        task.setEstimatedHours(request.getEstimatedHours());
        
        if (request.getDependencies() != null) {
            task.setDependencies(request.getDependencies());
        }
        
        task.setRecurring(request.isRecurring());
        task.setRecurringPattern(request.getRecurringPattern());
        
        return task;
    }
    
    public TaskResponse toResponse(Task task) {
        TaskResponse response = new TaskResponse();
        
        response.setTaskId(task.getTaskId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setTags(task.getTags());
        response.setDueDate(task.getDueDate());
        response.setCompletedAt(task.getCompletedAt());
        response.setEstimatedHours(task.getEstimatedHours());
        response.setActualHours(task.getActualHours());
        response.setDependencies(task.getDependencies());
        response.setRecurring(task.isRecurring());
        response.setRecurringPattern(task.getRecurringPattern());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setVersion(task.getVersion());
        response.setOverdue(task.isOverdue());
        
        return response;
    }
    
    public TaskResponse toResponse(Task task, List<Subtask> subtasks) {
        TaskResponse response = toResponse(task);
        
        if (subtasks != null) {
            List<SubtaskResponse> subtaskResponses = subtasks.stream()
                    .map(this::toSubtaskResponse)
                    .collect(Collectors.toList());
            
            response.setSubtasks(subtaskResponses);
            response.setTotalSubtasks(subtasks.size());
            response.setCompletedSubtasks((int) subtasks.stream()
                    .filter(Subtask::isCompleted)
                    .count());
        }
        
        return response;
    }
    
    public void updateEntity(Task task, UpdateTaskRequest request) {
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        
        if (request.getTags() != null) {
            task.setTags(request.getTags());
        }
        
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        
        if (request.getEstimatedHours() != null) {
            task.setEstimatedHours(request.getEstimatedHours());
        }
        
        if (request.getActualHours() != null) {
            task.setActualHours(request.getActualHours());
        }
        
        if (request.getDependencies() != null) {
            task.setDependencies(request.getDependencies());
        }
        
        if (request.getRecurring() != null) {
            task.setRecurring(request.getRecurring());
        }
        
        if (request.getRecurringPattern() != null) {
            task.setRecurringPattern(request.getRecurringPattern());
        }
    }
    
    private SubtaskResponse toSubtaskResponse(Subtask subtask) {
        SubtaskResponse response = new SubtaskResponse();
        
        response.setSubtaskId(subtask.getSubtaskId());
        response.setTaskId(subtask.getTaskId());
        response.setTitle(subtask.getTitle());
        response.setDescription(subtask.getDescription());
        response.setStatus(subtask.getStatus());
        response.setOrder(subtask.getOrder());
        response.setCompletedAt(subtask.getCompletedAt());
        response.setCreatedAt(subtask.getCreatedAt());
        response.setUpdatedAt(subtask.getUpdatedAt());
        
        return response;
    }
}