package com.taskmanagement.task.dto;

import com.taskmanagement.task.model.TaskStatus;
import com.taskmanagement.task.model.TaskPriority;
import com.taskmanagement.task.model.RecurringPattern;

import java.time.LocalDateTime;
import java.util.List;

public class TaskResponse {
    
    private String taskId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private List<String> tags;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private Double estimatedHours;
    private Double actualHours;
    private List<String> dependencies;
    private boolean isRecurring;
    private RecurringPattern recurringPattern;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
    private boolean isOverdue;
    private List<SubtaskResponse> subtasks;
    private int completedSubtasks;
    private int totalSubtasks;
    
    // Constructors
    public TaskResponse() {
    }
    
    // Getters and Setters
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
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public TaskPriority getPriority() {
        return priority;
    }
    
    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public Double getEstimatedHours() {
        return estimatedHours;
    }
    
    public void setEstimatedHours(Double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }
    
    public Double getActualHours() {
        return actualHours;
    }
    
    public void setActualHours(Double actualHours) {
        this.actualHours = actualHours;
    }
    
    public List<String> getDependencies() {
        return dependencies;
    }
    
    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }
    
    public boolean isRecurring() {
        return isRecurring;
    }
    
    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }
    
    public RecurringPattern getRecurringPattern() {
        return recurringPattern;
    }
    
    public void setRecurringPattern(RecurringPattern recurringPattern) {
        this.recurringPattern = recurringPattern;
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
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    public boolean isOverdue() {
        return isOverdue;
    }
    
    public void setOverdue(boolean overdue) {
        isOverdue = overdue;
    }
    
    public List<SubtaskResponse> getSubtasks() {
        return subtasks;
    }
    
    public void setSubtasks(List<SubtaskResponse> subtasks) {
        this.subtasks = subtasks;
    }
    
    public int getCompletedSubtasks() {
        return completedSubtasks;
    }
    
    public void setCompletedSubtasks(int completedSubtasks) {
        this.completedSubtasks = completedSubtasks;
    }
    
    public int getTotalSubtasks() {
        return totalSubtasks;
    }
    
    public void setTotalSubtasks(int totalSubtasks) {
        this.totalSubtasks = totalSubtasks;
    }
}