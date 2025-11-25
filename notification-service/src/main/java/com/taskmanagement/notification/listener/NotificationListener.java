package com.taskmanagement.notification.listener;

import com.taskmanagement.notification.config.RabbitMQConfig;
import com.taskmanagement.notification.dto.NotificationRequest;
import com.taskmanagement.notification.model.Notification;
import com.taskmanagement.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {
    
    private final NotificationService notificationService;
    
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotificationRequest(NotificationRequest request) {
        log.info("Received notification request for user: {} with type: {}", 
                request.getUserId(), request.getType());
        
        try {
            notificationService.createNotification(request);
        } catch (Exception e) {
            log.error("Failed to process notification request for user: {}", request.getUserId(), e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
    
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void handleEmailNotification(Notification notification) {
        log.info("Received email notification: {}", notification.getId());
        
        try {
            notificationService.processNotification(notification);
        } catch (Exception e) {
            log.error("Failed to process email notification: {}", notification.getId(), e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
}