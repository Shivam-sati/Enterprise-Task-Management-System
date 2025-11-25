package com.taskmanagement.notification.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notification_preferences")
public class NotificationPreferences {
    
    @Id
    private String id;
    
    private String userId;
    
    // Channel preferences for each notification type
    private Map<NotificationType, Set<NotificationChannel>> channelPreferences;
    
    // Global channel settings
    private boolean emailEnabled;
    private boolean inAppEnabled;
    private boolean smsEnabled;
    private boolean pushEnabled;
    
    // Timing preferences
    private boolean immediateNotifications;
    private String digestFrequency; // "daily", "weekly", "never"
    private String quietHoursStart; // "22:00"
    private String quietHoursEnd; // "08:00"
    private String timezone;
    
    // Specific preferences
    private boolean taskReminders;
    private int reminderMinutesBefore; // Default 30 minutes
    private boolean overdueNotifications;
    private boolean collaborationNotifications;
    private boolean systemNotifications;
}