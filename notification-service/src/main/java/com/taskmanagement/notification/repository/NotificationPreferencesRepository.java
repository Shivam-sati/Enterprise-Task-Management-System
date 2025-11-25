package com.taskmanagement.notification.repository;

import com.taskmanagement.notification.model.NotificationPreferences;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferencesRepository extends MongoRepository<NotificationPreferences, String> {
    
    Optional<NotificationPreferences> findByUserId(String userId);
    
    boolean existsByUserId(String userId);
}