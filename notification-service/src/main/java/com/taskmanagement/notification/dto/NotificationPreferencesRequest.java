package com.taskmanagement.notification.dto;

import com.taskmanagement.notification.model.NotificationType;
import com.taskmanagement.notification.model.NotificationChannel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferencesRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    private Map<NotificationType, Set<NotificationChannel>> channelPreferences;
    
    private boolean emailEnabled;
    private boolean inAppEnabled;
    private boolean smsEnabled;
    private boolean pushEnabled;
    
    private boolean immediateNotifications;
    private String digestFrequency;
    private String quietHoursStart;
    private String quietHoursEnd;
    private String timezone;
    
    private boolean taskReminders;
    private int reminderMinutesBefore;
    private boolean overdueNotifications;
    private boolean collaborationNotifications;
    private boolean systemNotifications;
}