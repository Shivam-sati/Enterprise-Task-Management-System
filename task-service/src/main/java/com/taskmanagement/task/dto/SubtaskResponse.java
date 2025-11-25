package com.taskmanagement.task.dto;

import com.taskmanagement.task.model.SubtaskStatus;

import java.time.LocalDateTime;

public class SubtaskResponse {
    
    private String subtaskId;
    private String taskId;
    private String title;
    private String description;
    private SubtaskStatus status;
    private int order;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public SubtaskResponse() {
    }
    
    // Getters and Setters
    public String getSubtaskId() {
        return subtaskId;
    }
    
    public void setSubtaskId(String subtaskId) {
        this.subtaskId = subtaskId;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public SubtaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(SubtaskStatus status) {
        this.status = status;
    }
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}