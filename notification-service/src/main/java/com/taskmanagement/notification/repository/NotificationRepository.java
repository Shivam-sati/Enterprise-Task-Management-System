package com.taskmanagement.notification.repository;

import com.taskmanagement.notification.model.Notification;
import com.taskmanagement.notification.model.NotificationStatus;
import com.taskmanagement.notification.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    List<Notification> findByUserIdAndStatus(String userId, NotificationStatus status);
    
    Page<Notification> findByUserId(String userId, Pageable pageable);
    
    List<Notification> findByStatusAndScheduledAtBefore(NotificationStatus status, LocalDateTime dateTime);
    
    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);
    
    @Query("{ 'relatedEntityId': ?0, 'relatedEntityType': ?1 }")
    List<Notification> findByRelatedEntity(String entityId, String entityType);
    
    List<Notification> findByUserIdAndTypeAndCreatedAtBetween(
        String userId, NotificationType type, LocalDateTime start, LocalDateTime end);
    
    long countByUserIdAndStatus(String userId, NotificationStatus status);
}