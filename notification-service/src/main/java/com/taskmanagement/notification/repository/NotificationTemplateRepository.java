package com.taskmanagement.notification.repository;

import com.taskmanagement.notification.model.NotificationTemplate;
import com.taskmanagement.notification.model.NotificationType;
import com.taskmanagement.notification.model.NotificationChannel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends MongoRepository<NotificationTemplate, String> {
    
    Optional<NotificationTemplate> findByTypeAndChannelAndActiveTrue(
        NotificationType type, NotificationChannel channel);
    
    List<NotificationTemplate> findByTypeAndActiveTrue(NotificationType type);
    
    List<NotificationTemplate> findByActiveTrue();
}