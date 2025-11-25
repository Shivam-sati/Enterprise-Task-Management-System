package com.taskmanagement.task.mapper;

import com.taskmanagement.task.dto.CreateSubtaskRequest;
import com.taskmanagement.task.dto.SubtaskResponse;
import com.taskmanagement.task.model.Subtask;
import org.springframework.stereotype.Component;

@Component
public class SubtaskMapper {
    
    public Subtask toEntity(CreateSubtaskRequest request, String taskId, int order) {
        Subtask subtask = new Subtask(taskId, request.getTitle(), order);
        
        if (request.getDescription() != null) {
            subtask.setDescription(request.getDescription());
        }
        
        return subtask;
    }
    
    public SubtaskResponse toResponse(Subtask subtask) {
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