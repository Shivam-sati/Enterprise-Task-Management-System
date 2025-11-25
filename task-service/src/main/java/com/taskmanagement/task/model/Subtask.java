package com.taskmanagement.task.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "subtasks")
public class Subtask {
    
    @Id
    private String id;
    
    @NotBlank
    @Indexed(unique = true)
    private String subtaskId;
    
    @NotBlank
    @Indexed
    private String taskId;
    
    @NotBlank
    @Size(min = 1, max = 200)
    private String title;
    
    @Size(max = 1000)
    private String description;
    
    @NotNull
    private SubtaskStatus status;
    
    @NotNull
    private int order;
    
    private LocalDateTime completedAt;
    
    @Indexed
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Constructors
    public Subtask() {
        this.subtaskId = UUID.randomUUID().toString();
        this.status = SubtaskStatus.TODO;
        this.order = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Subtask(String taskId, String title) {
        this();
        this.taskId = taskId;
        this.title = title;
    }
    
    public Subtask(String taskId, String title, int order) {
        this(taskId, title);
        this.order = order;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
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
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
    
    public SubtaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(SubtaskStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status == SubtaskStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        } else if (status != SubtaskStatus.COMPLETED) {
            this.completedAt = null;
        }
    }
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
        this.updatedAt = LocalDateTime.now();
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
    
    // Helper methods
    public boolean isCompleted() {
        return status == SubtaskStatus.COMPLETED;
    }
    
    public void complete() {
        setStatus(SubtaskStatus.COMPLETED);
    }
    
    public void reopen() {
        setStatus(SubtaskStatus.TODO);
    }
    
    @Override
    public String toString() {
        return "Subtask{" +
                "subtaskId='" + subtaskId + '\'' +
                ", taskId='" + taskId + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", order=" + order +
                '}';
    }
}