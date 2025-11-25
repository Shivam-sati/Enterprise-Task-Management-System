package com.taskmanagement.task.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "tasks")
public class Task {
    
    @Id
    private String id;
    
    @NotBlank
    @Indexed(unique = true)
    private String taskId;
    
    @NotBlank
    @Indexed
    private String userId;
    
    @NotBlank
    @Size(min = 1, max = 200)
    @TextIndexed(weight = 2)
    private String title;
    
    @Size(max = 2000)
    @TextIndexed
    private String description;
    
    @NotNull
    @Indexed
    private TaskStatus status;
    
    @NotNull
    @Indexed
    private TaskPriority priority;
    
    @Indexed
    private List<String> tags;
    
    @Indexed
    private LocalDateTime dueDate;
    
    private LocalDateTime completedAt;
    
    private Double estimatedHours;
    
    private Double actualHours;
    
    private List<String> dependencies;
    
    private boolean isRecurring;
    
    private RecurringPattern recurringPattern;
    
    @Indexed
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    // Constructors
    public Task() {
        this.taskId = UUID.randomUUID().toString();
        this.status = TaskStatus.TODO;
        this.priority = TaskPriority.MEDIUM;
        this.tags = new ArrayList<>();
        this.dependencies = new ArrayList<>();
        this.isRecurring = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Task(String userId, String title, String description) {
        this();
        this.userId = userId;
        this.title = title;
        this.description = description;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status == TaskStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        } else if (status != TaskStatus.COMPLETED) {
            this.completedAt = null;
        }
    }
    
    public TaskPriority getPriority() {
        return priority;
    }
    
    public void setPriority(TaskPriority priority) {
        this.priority = priority;
        this.updatedAt = LocalDateTime.now();
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = LocalDateTime.now();
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
        this.updatedAt = LocalDateTime.now();
    }
    
    public Double getActualHours() {
        return actualHours;
    }
    
    public void setActualHours(Double actualHours) {
        this.actualHours = actualHours;
        this.updatedAt = LocalDateTime.now();
    }
    
    public List<String> getDependencies() {
        return dependencies;
    }
    
    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isRecurring() {
        return isRecurring;
    }
    
    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
        this.updatedAt = LocalDateTime.now();
    }
    
    public RecurringPattern getRecurringPattern() {
        return recurringPattern;
    }
    
    public void setRecurringPattern(RecurringPattern recurringPattern) {
        this.recurringPattern = recurringPattern;
        this.updatedAt = LocalDateTime.now();
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
    
    // Helper methods
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !this.tags.contains(tag)) {
            this.tags.add(tag.trim());
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public void removeTag(String tag) {
        if (this.tags.remove(tag)) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public void addDependency(String taskId) {
        if (taskId != null && !taskId.equals(this.taskId) && !this.dependencies.contains(taskId)) {
            this.dependencies.add(taskId);
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public void removeDependency(String taskId) {
        if (this.dependencies.remove(taskId)) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && status != TaskStatus.COMPLETED;
    }
    
    public boolean canBeCompleted() {
        return status != TaskStatus.COMPLETED && status != TaskStatus.CANCELLED;
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "taskId='" + taskId + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", dueDate=" + dueDate +
                '}';
    }
}