package com.taskmanagement.task.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "tags")
public class Tag {
    
    @Id
    private String id;
    
    @NotBlank
    @Indexed(unique = true)
    private String tagId;
    
    @NotBlank
    @Indexed
    private String userId;
    
    @NotBlank
    @Size(min = 1, max = 50)
    @Indexed(unique = true)
    private String name;
    
    @Size(max = 200)
    private String description;
    
    private String color;
    
    private int usageCount;
    
    @Indexed
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Constructors
    public Tag() {
        this.tagId = UUID.randomUUID().toString();
        this.usageCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Tag(String userId, String name) {
        this();
        this.userId = userId;
        this.name = name;
    }
    
    public Tag(String userId, String name, String description, String color) {
        this(userId, name);
        this.description = description;
        this.color = color;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTagId() {
        return tagId;
    }
    
    public void setTagId(String tagId) {
        this.tagId = tagId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
        this.updatedAt = LocalDateTime.now();
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
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
    
    // Helper methods
    public void incrementUsage() {
        this.usageCount++;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void decrementUsage() {
        if (this.usageCount > 0) {
            this.usageCount--;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    @Override
    public String toString() {
        return "Tag{" +
                "tagId='" + tagId + '\'' +
                ", name='" + name + '\'' +
                ", usageCount=" + usageCount +
                '}';
    }
}