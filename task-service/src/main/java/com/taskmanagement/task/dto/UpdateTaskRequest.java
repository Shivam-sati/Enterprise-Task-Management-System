package com.taskmanagement.task.dto;

import com.taskmanagement.task.model.TaskStatus;
import com.taskmanagement.task.model.TaskPriority;
import com.taskmanagement.task.model.RecurringPattern;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public class UpdateTaskRequest {
    
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    private TaskStatus status;
    
    private TaskPriority priority;
    
    private List<String> tags;
    
    private LocalDateTime dueDate;
    
    private Double estimatedHours;
    
    private Double actualHours;
    
    private List<String> dependencies;
    
    private Boolean isRecurring;
    
    private RecurringPattern recurringPattern;
    
    // Constructors
    public UpdateTaskRequest() {
    }
    
    // Getters and Setters
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
    
    public Boolean getRecurring() {
        return isRecurring;
    }
    
    public void setRecurring(Boolean recurring) {
        isRecurring = recurring;
    }
    
    public RecurringPattern getRecurringPattern() {
        return recurringPattern;
    }
    
    public void setRecurringPattern(RecurringPattern recurringPattern) {
        this.recurringPattern = recurringPattern;
    }
}