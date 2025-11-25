package com.taskmanagement.notification.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notifications")
public class Notification {
    
    @Id
    private String id;
    
    private String userId;
    private String recipientEmail;
    private NotificationType type;
    private NotificationChannel channel;
    private NotificationStatus status;
    
    private String subject;
    private String message;
    private String templateId;
    private Map<String, Object> templateData;
    
    private String relatedEntityId; // Task ID, Project ID, etc.
    private String relatedEntityType; // "task", "project", etc.
    
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    
    private int retryCount;
    private String errorMessage;
    
    private Map<String, Object> metadata;
}