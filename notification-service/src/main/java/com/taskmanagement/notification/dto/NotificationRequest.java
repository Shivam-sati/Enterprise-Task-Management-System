package com.taskmanagement.notification.dto;

import com.taskmanagement.notification.model.NotificationType;
import com.taskmanagement.notification.model.NotificationChannel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @Email(message = "Valid email is required")
    private String recipientEmail;
    
    @NotNull(message = "Notification type is required")
    private NotificationType type;
    
    private Set<NotificationChannel> channels;
    
    private String subject;
    private String message;
    private String templateId;
    private Map<String, Object> templateData;
    
    private String relatedEntityId;
    private String relatedEntityType;
    
    private LocalDateTime scheduledAt;
    private Map<String, Object> metadata;
}