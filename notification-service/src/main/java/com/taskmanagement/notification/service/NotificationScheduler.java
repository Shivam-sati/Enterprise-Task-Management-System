package com.taskmanagement.notification.service;

import com.taskmanagement.notification.model.Notification;
import com.taskmanagement.notification.model.NotificationStatus;
import com.taskmanagement.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "notification.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationScheduler {
    
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    
    @Scheduled(fixedDelay = 60000) // Run every minute
    public void processScheduledNotifications() {
        log.debug("Processing scheduled notifications");
        
        LocalDateTime now = LocalDateTime.now();
        List<Notification> scheduledNotifications = notificationRepository
                .findByStatusAndScheduledAtBefore(NotificationStatus.PENDING, now);
        
        log.info("Found {} scheduled notifications to process", scheduledNotifications.size());
        
        for (Notification notification : scheduledNotifications) {
            try {
                notificationService.processNotification(notification);
            } catch (Exception e) {
                log.error("Failed to process scheduled notification: {}", notification.getId(), e);
            }
        }
    }
    
    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void retryFailedNotifications() {
        log.debug("Processing failed notifications for retry");
        
        List<Notification> failedNotifications = notificationRepository
                .findByStatusAndRetryCountLessThan(NotificationStatus.RETRYING, 3);
        
        log.info("Found {} failed notifications to retry", failedNotifications.size());
        
        for (Notification notification : failedNotifications) {
            try {
                // Check if it's time to retry
                if (notification.getScheduledAt() != null && 
                    notification.getScheduledAt().isBefore(LocalDateTime.now())) {
                    notificationService.processNotification(notification);
                }
            } catch (Exception e) {
                log.error("Failed to retry notification: {}", notification.getId(), e);
            }
        }
    }
    
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void cleanupOldNotifications() {
        log.info("Starting cleanup of old notifications");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        try {
            // This would require a custom repository method
            // For now, we'll just log the intent
            log.info("Would cleanup notifications older than: {}", cutoffDate);
            // notificationRepository.deleteByCreatedAtBefore(cutoffDate);
        } catch (Exception e) {
            log.error("Failed to cleanup old notifications", e);
        }
    }
}