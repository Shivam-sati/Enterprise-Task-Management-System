package com.taskmanagement.task.controller;

import com.taskmanagement.task.dto.*;
import com.taskmanagement.task.model.TaskStatus;
import com.taskmanagement.task.model.TaskPriority;
import com.taskmanagement.task.service.TaskService;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
    
    private final TaskService taskService;
    
    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }
    
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Creating task for user: {}", userId);
        
        TaskResponse response = taskService.createTask(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(
            @PathVariable String taskId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("Getting task: {} for user: {}", taskId, userId);
        
        TaskResponse response = taskService.getTaskById(taskId, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("Getting tasks for user: {} (page: {}, size: {})", userId, page, size);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<TaskResponse> response = taskService.getTasksByUserId(userId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/filter")
    public ResponseEntity<Page<TaskResponse>> getFilteredTasks(
            @RequestBody TaskFilterRequest filterRequest,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("Getting filtered tasks for user: {}", userId);
        
        Page<TaskResponse> response = taskService.getFilteredTasks(userId, filterRequest);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<TaskResponse>> searchTasks(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("Searching tasks for user: {} with query: {}", userId, q);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<TaskResponse> response = taskService.searchTasks(userId, q, pageable);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Updating task: {} for user: {}", taskId, userId);
        
        TaskResponse response = taskService.updateTask(taskId, userId, request);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable String taskId,
            @RequestParam TaskStatus status,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Updating task status: {} to {} for user: {}", taskId, status, userId);
        
        TaskResponse response = taskService.updateTaskStatus(taskId, userId, status);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable String taskId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Deleting task: {} for user: {}", taskId, userId);
        
        taskService.deleteTask(taskId, userId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{taskId}/dependencies")
    public ResponseEntity<TaskResponse> addDependency(
            @PathVariable String taskId,
            @RequestParam String dependencyId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Adding dependency: {} -> {} for user: {}", taskId, dependencyId, userId);
        
        TaskResponse response = taskService.addDependency(taskId, userId, dependencyId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{taskId}/dependencies/{dependencyId}")
    public ResponseEntity<TaskResponse> removeDependency(
            @PathVariable String taskId,
            @PathVariable String dependencyId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Removing dependency: {} -> {} for user: {}", taskId, dependencyId, userId);
        
        TaskResponse response = taskService.removeDependency(taskId, userId, dependencyId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/overdue")
    public ResponseEntity<List<TaskResponse>> getOverdueTasks(Authentication authentication) {
        String userId = authentication.getName();
        logger.debug("Getting overdue tasks for user: {}", userId);
        
        List<TaskResponse> response = taskService.getOverdueTasks(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskResponse>> getTasksByStatus(
            @PathVariable TaskStatus status,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("Getting tasks by status: {} for user: {}", status, userId);
        
        List<TaskResponse> response = taskService.getTasksByStatus(userId, status);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<TaskResponse>> getTasksByPriority(
            @PathVariable TaskPriority priority,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("Getting tasks by priority: {} for user: {}", priority, userId);
        
        List<TaskResponse> response = taskService.getTasksByPriority(userId, priority);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<TaskResponse>> getTasksByTag(
            @PathVariable String tag,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("Getting tasks by tag: {} for user: {}", tag, userId);
        
        List<TaskResponse> response = taskService.getTasksByTag(userId, tag);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/ready")
    public ResponseEntity<List<TaskResponse>> getReadyTasks(Authentication authentication) {
        String userId = authentication.getName();
        logger.debug("Getting ready tasks for user: {}", userId);
        
        List<TaskResponse> response = taskService.getReadyTasks(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getTaskStatistics(Authentication authentication) {
        String userId = authentication.getName();
        logger.debug("Getting task statistics for user: {}", userId);
        
        Map<String, Long> response = taskService.getTaskStatistics(userId);
        return ResponseEntity.ok(response);
    }
}