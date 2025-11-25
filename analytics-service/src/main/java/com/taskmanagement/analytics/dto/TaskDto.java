package com.taskmanagement.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {
    private String taskId;
    private String userId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private List<String> tags;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private Double estimatedHours;
    private Double actualHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}