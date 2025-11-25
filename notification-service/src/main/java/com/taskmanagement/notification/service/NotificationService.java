package com.taskmanagement.notification.service;

import com.taskmanagement.notification.dto.NotificationRequest;
import com.taskmanagement.notification.dto.NotificationResponse;
import com.taskmanagement.notification.model.*;
import com.taskmanagement.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesService preferencesService;
    private final EmailService emailService;
    private final NotificationPublisher notificationPublisher;
    
    public NotificationResponse createNotification(NotificationRequest request) {
        log.info("Creating notification for user: {} with type: {}", request.getUserId(), request.getType());
        
        // Get user preferences to determine which channels to use
        NotificationPreferences preferences = preferencesService.getUserPreferences(request.getUserId());
        
        // Create notifications for each preferred channel
        List<Notification> notifications = createNotificationsForChannels(request, preferences);
        
        // Save all notifications
        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
        
        // Process immediate notifications
        savedNotifications.forEach(notification -> {
            if (notification.getScheduledAt() == null || 
                notification.getScheduledAt().isBefore(LocalDateTime.now().plusMinutes(1))) {
                processNotification(notification);
            }
        });
        
        // Return the first notification as response (or could return all)
        return mapToResponse(savedNotifications.get(0));
    }
    
    private List<Notification> createNotificationsForChannels(NotificationRequest request, 
                                                            NotificationPreferences preferences) {
        return determineChannels(request, preferences).stream()
                .map(channel -> createNotificationForChannel(request, channel))
                .collect(Collectors.toList());
    }
    
    private List<NotificationChannel> determineChannels(NotificationRequest request, 
                                                      NotificationPreferences preferences) {
        if (request.getChannels() != null && !request.getChannels().isEmpty()) {
            return request.getChannels().stream().collect(Collectors.toList());
        }
        
        // Use preferences to determine channels
        return preferences.getChannelPreferences()
                .getOrDefault(request.getType(), Set.of(NotificationChannel.EMAIL))
                .stream().collect(Collectors.toList());
    }
    
    private Notification createNotificationForChannel(NotificationRequest request, 
                                                    NotificationChannel channel) {
        return Notification.builder()
                .userId(request.getUserId())
                .recipientEmail(request.getRecipientEmail())
                .type(request.getType())
                .channel(channel)
                .status(NotificationStatus.PENDING)
                .subject(request.getSubject())
                .message(request.getMessage())
                .templateId(request.getTemplateId())
                .templateData(request.getTemplateData())
                .relatedEntityId(request.getRelatedEntityId())
                .relatedEntityType(request.getRelatedEntityType())
                .createdAt(LocalDateTime.now())
                .scheduledAt(request.getScheduledAt())
                .retryCount(0)
                .metadata(request.getMetadata())
                .build();
    }
    
    public void processNotification(Notification notification) {
        log.info("Processing notification: {} for channel: {}", notification.getId(), notification.getChannel());
        
        try {
            switch (notification.getChannel()) {
                case EMAIL:
                    emailService.sendEmail(notification);
                    break;
                case IN_APP:
                    // Handle in-app notifications
                    handleInAppNotification(notification);
                    break;
                case SMS:
                    // Handle SMS notifications
                    handleSmsNotification(notification);
                    break;
                case PUSH:
                    // Handle push notifications
                    handlePushNotification(notification);
                    break;
            }
            
            updateNotificationStatus(notification.getId(), NotificationStatus.SENT);
            
        } catch (Exception e) {
            log.error("Failed to process notification: {}", notification.getId(), e);
            handleNotificationFailure(notification, e.getMessage());
        }
    }
    
    private void handleInAppNotification(Notification notification) {
        // Publish to in-app notification queue or websocket
        notificationPublisher.publishInAppNotification(notification);
        log.info("In-app notification sent for: {}", notification.getId());
    }
    
    private void handleSmsNotification(Notification notification) {
        // Implement SMS sending logic
        log.info("SMS notification sent for: {}", notification.getId());
    }
    
    private void handlePushNotification(Notification notification) {
        // Implement push notification logic
        log.info("Push notification sent for: {}", notification.getId());
    }
    
    public void updateNotificationStatus(String notificationId, NotificationStatus status) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setStatus(status);
            
            if (status == NotificationStatus.SENT) {
                notification.setSentAt(LocalDateTime.now());
            } else if (status == NotificationStatus.DELIVERED) {
                notification.setDeliveredAt(LocalDateTime.now());
            }
            
            notificationRepository.save(notification);
        }
    }
    
    private void handleNotificationFailure(Notification notification, String errorMessage) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setErrorMessage(errorMessage);
        notification.setRetryCount(notification.getRetryCount() + 1);
        
        notificationRepository.save(notification);
        
        // Schedule retry if under max attempts
        if (notification.getRetryCount() < 3) {
            scheduleRetry(notification);
        }
    }
    
    private void scheduleRetry(Notification notification) {
        // Calculate retry delay (exponential backoff)
        long delayMinutes = (long) Math.pow(2, notification.getRetryCount());
        LocalDateTime retryAt = LocalDateTime.now().plusMinutes(delayMinutes);
        
        notification.setScheduledAt(retryAt);
        notification.setStatus(NotificationStatus.RETRYING);
        
        notificationRepository.save(notification);
        log.info("Scheduled retry for notification: {} at: {}", notification.getId(), retryAt);
    }
    
    public Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserId(userId, pageable);
        return notifications.map(this::mapToResponse);
    }
    
    public List<NotificationResponse> getNotificationsByEntity(String entityId, String entityType) {
        List<Notification> notifications = notificationRepository.findByRelatedEntity(entityId, entityType);
        return notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT);
    }
    
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .recipientEmail(notification.getRecipientEmail())
                .type(notification.getType())
                .channel(notification.getChannel())
                .status(notification.getStatus())
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .relatedEntityId(notification.getRelatedEntityId())
                .relatedEntityType(notification.getRelatedEntityType())
                .createdAt(notification.getCreatedAt())
                .scheduledAt(notification.getScheduledAt())
                .sentAt(notification.getSentAt())
                .deliveredAt(notification.getDeliveredAt())
                .retryCount(notification.getRetryCount())
                .errorMessage(notification.getErrorMessage())
                .metadata(notification.getMetadata())
                .build();
    }
}