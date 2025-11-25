package com.taskmanagement.notification.dto;

import com.taskmanagement.notification.model.NotificationType;
import com.taskmanagement.notification.model.NotificationChannel;
import com.taskmanagement.notification.model.NotificationStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    
    private String id;
    private String userId;
    private String recipientEmail;
    private NotificationType type;
    private NotificationChannel channel;
    private NotificationStatus status;
    
    private String subject;
    private String message;
    
    private String relatedEntityId;
    private String relatedEntityType;
    
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    
    private int retryCount;
    private String errorMessage;
    
    private Map<String, Object> metadata;
}