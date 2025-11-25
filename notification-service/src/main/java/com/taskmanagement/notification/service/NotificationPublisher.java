package com.taskmanagement.notification.service;

import com.taskmanagement.notification.config.RabbitMQConfig;
import com.taskmanagement.notification.dto.NotificationRequest;
import com.taskmanagement.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishNotificationRequest(NotificationRequest request) {
        log.info("Publishing notification request for user: {} with type: {}", 
                request.getUserId(), request.getType());
        
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    "notification.create",
                    request
            );
            log.debug("Notification request published successfully");
        } catch (Exception e) {
            log.error("Failed to publish notification request", e);
            throw new RuntimeException("Failed to publish notification request", e);
        }
    }
    
    public void publishEmailNotification(Notification notification) {
        log.info("Publishing email notification: {}", notification.getId());
        
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.EMAIL_SEND_KEY,
                    notification
            );
            log.debug("Email notification published successfully");
        } catch (Exception e) {
            log.error("Failed to publish email notification", e);
            throw new RuntimeException("Failed to publish email notification", e);
        }
    }
    
    public void publishInAppNotification(Notification notification) {
        log.info("Publishing in-app notification: {}", notification.getId());
        
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    "notification.inapp",
                    notification
            );
            log.debug("In-app notification published successfully");
        } catch (Exception e) {
            log.error("Failed to publish in-app notification", e);
            throw new RuntimeException("Failed to publish in-app notification", e);
        }
    }
    
    public void publishTaskEvent(String eventType, String taskId, String userId, Object eventData) {
        log.info("Publishing task event: {} for task: {}", eventType, taskId);
        
        try {
            String routingKey = "task." + eventType.toLowerCase();
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TASK_EXCHANGE,
                    routingKey,
                    eventData
            );
            log.debug("Task event published successfully with routing key: {}", routingKey);
        } catch (Exception e) {
            log.error("Failed to publish task event", e);
            throw new RuntimeException("Failed to publish task event", e);
        }
    }
}