package com.taskmanagement.notification.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notification_templates")
public class NotificationTemplate {
    
    @Id
    private String id;
    
    private String name;
    private NotificationType type;
    private NotificationChannel channel;
    
    private String subject;
    private String htmlContent;
    private String textContent;
    
    private String sendGridTemplateId; // For SendGrid dynamic templates
    
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}