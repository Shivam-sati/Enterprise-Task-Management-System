package com.taskmanagement.task.controller;

import com.taskmanagement.task.dto.CreateSubtaskRequest;
import com.taskmanagement.task.dto.SubtaskResponse;
import com.taskmanagement.task.model.SubtaskStatus;
import com.taskmanagement.task.service.SubtaskService;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/subtasks")
public class SubtaskController {
    
    private static final Logger logger = LoggerFactory.getLogger(SubtaskController.class);
    
    private final SubtaskService subtaskService;
    
    @Autowired
    public SubtaskController(SubtaskService subtaskService) {
        this.subtaskService = subtaskService;
    }
    
    @PostMapping
    public ResponseEntity<SubtaskResponse> createSubtask(
            @PathVariable String taskId,
            @Valid @RequestBody CreateSubtaskRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Creating subtask for task: {} by user: {}", taskId, userId);
        
        SubtaskResponse response = subtaskService.createSubtask(taskId, userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<SubtaskResponse>> getSubtasks(
            @PathVariable String taskId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("Getting subtasks for task: {} by user: {}", taskId, userId);
        
        List<SubtaskResponse> response = subtaskService.getSubtasksByTaskId(taskId, userId);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{subtaskId}/status")
    public ResponseEntity<SubtaskResponse> updateSubtaskStatus(
            @PathVariable String taskId,
            @PathVariable String subtaskId,
            @RequestParam SubtaskStatus status,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Updating subtask status: {} to {} for task: {} by user: {}", 
                   subtaskId, status, taskId, userId);
        
        SubtaskResponse response = subtaskService.updateSubtaskStatus(subtaskId, taskId, userId, status);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{subtaskId}")
    public ResponseEntity<Void> deleteSubtask(
            @PathVariable String taskId,
            @PathVariable String subtaskId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Deleting subtask: {} from task: {} by user: {}", subtaskId, taskId, userId);
        
        subtaskService.deleteSubtask(subtaskId, taskId, userId);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/reorder")
    public ResponseEntity<List<SubtaskResponse>> reorderSubtasks(
            @PathVariable String taskId,
            @RequestBody List<String> subtaskIds,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Reordering subtasks for task: {} by user: {}", taskId, userId);
        
        List<SubtaskResponse> response = subtaskService.reorderSubtasks(taskId, subtaskIds);
        return ResponseEntity.ok(response);
    }
}