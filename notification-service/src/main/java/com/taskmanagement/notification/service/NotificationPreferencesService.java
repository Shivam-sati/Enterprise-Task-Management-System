package com.taskmanagement.notification.service;

import com.taskmanagement.notification.dto.NotificationPreferencesRequest;
import com.taskmanagement.notification.model.*;
import com.taskmanagement.notification.repository.NotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferencesService {
    
    private final NotificationPreferencesRepository preferencesRepository;
    
    public NotificationPreferences getUserPreferences(String userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }
    
    public NotificationPreferences updatePreferences(NotificationPreferencesRequest request) {
        log.info("Updating notification preferences for user: {}", request.getUserId());
        
        NotificationPreferences preferences = preferencesRepository.findByUserId(request.getUserId())
                .orElse(createDefaultPreferences(request.getUserId()));
        
        // Update preferences from request
        if (request.getChannelPreferences() != null) {
            preferences.setChannelPreferences(request.getChannelPreferences());
        }
        
        preferences.setEmailEnabled(request.isEmailEnabled());
        preferences.setInAppEnabled(request.isInAppEnabled());
        preferences.setSmsEnabled(request.isSmsEnabled());
        preferences.setPushEnabled(request.isPushEnabled());
        
        preferences.setImmediateNotifications(request.isImmediateNotifications());
        preferences.setDigestFrequency(request.getDigestFrequency());
        preferences.setQuietHoursStart(request.getQuietHoursStart());
        preferences.setQuietHoursEnd(request.getQuietHoursEnd());
        preferences.setTimezone(request.getTimezone());
        
        preferences.setTaskReminders(request.isTaskReminders());
        preferences.setReminderMinutesBefore(request.getReminderMinutesBefore());
        preferences.setOverdueNotifications(request.isOverdueNotifications());
        preferences.setCollaborationNotifications(request.isCollaborationNotifications());
        preferences.setSystemNotifications(request.isSystemNotifications());
        
        return preferencesRepository.save(preferences);
    }
    
    private NotificationPreferences createDefaultPreferences(String userId) {
        log.info("Creating default notification preferences for user: {}", userId);
        
        Map<NotificationType, Set<NotificationChannel>> defaultChannels = new HashMap<>();
        
        // Set default channels for each notification type
        defaultChannels.put(NotificationType.TASK_CREATED, Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP));
        defaultChannels.put(NotificationType.TASK_UPDATED, Set.of(NotificationChannel.IN_APP));
        defaultChannels.put(NotificationType.TASK_COMPLETED, Set.of(NotificationChannel.IN_APP));
        defaultChannels.put(NotificationType.TASK_ASSIGNED, Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP));
        defaultChannels.put(NotificationType.TASK_DUE_REMINDER, Set.of(NotificationChannel.EMAIL, NotificationChannel.PUSH));
        defaultChannels.put(NotificationType.TASK_OVERDUE, Set.of(NotificationChannel.EMAIL, NotificationChannel.PUSH));
        defaultChannels.put(NotificationType.SUBTASK_COMPLETED, Set.of(NotificationChannel.IN_APP));
        defaultChannels.put(NotificationType.COMMENT_ADDED, Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP));
        defaultChannels.put(NotificationType.COLLABORATION_INVITE, Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP));
        defaultChannels.put(NotificationType.SYSTEM_NOTIFICATION, Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP));
        
        NotificationPreferences preferences = NotificationPreferences.builder()
                .userId(userId)
                .channelPreferences(defaultChannels)
                .emailEnabled(true)
                .inAppEnabled(true)
                .smsEnabled(false)
                .pushEnabled(true)
                .immediateNotifications(true)
                .digestFrequency("daily")
                .quietHoursStart("22:00")
                .quietHoursEnd("08:00")
                .timezone("UTC")
                .taskReminders(true)
                .reminderMinutesBefore(30)
                .overdueNotifications(true)
                .collaborationNotifications(true)
                .systemNotifications(true)
                .build();
        
        return preferencesRepository.save(preferences);
    }
    
    public boolean isChannelEnabledForUser(String userId, NotificationChannel channel) {
        NotificationPreferences preferences = getUserPreferences(userId);
        
        switch (channel) {
            case EMAIL:
                return preferences.isEmailEnabled();
            case IN_APP:
                return preferences.isInAppEnabled();
            case SMS:
                return preferences.isSmsEnabled();
            case PUSH:
                return preferences.isPushEnabled();
            default:
                return false;
        }
    }
    
    public boolean shouldSendNotification(String userId, NotificationType type, NotificationChannel channel) {
        NotificationPreferences preferences = getUserPreferences(userId);
        
        // Check if channel is enabled globally
        if (!isChannelEnabledForUser(userId, channel)) {
            return false;
        }
        
        // Check if channel is enabled for this notification type
        Set<NotificationChannel> enabledChannels = preferences.getChannelPreferences()
                .getOrDefault(type, Set.of());
        
        return enabledChannels.contains(channel);
    }
}