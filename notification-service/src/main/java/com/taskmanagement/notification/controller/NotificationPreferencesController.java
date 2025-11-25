package com.taskmanagement.notification.controller;

import com.taskmanagement.notification.dto.NotificationPreferencesRequest;
import com.taskmanagement.notification.model.NotificationPreferences;
import com.taskmanagement.notification.service.NotificationPreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/notifications/preferences")
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferencesController {
    
    private final NotificationPreferencesService preferencesService;
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<NotificationPreferences> getUserPreferences(@PathVariable String userId) {
        log.info("Fetching notification preferences for user: {}", userId);
        
        NotificationPreferences preferences = preferencesService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }
    
    @PutMapping
    public ResponseEntity<NotificationPreferences> updatePreferences(
            @Valid @RequestBody NotificationPreferencesRequest request) {
        log.info("Updating notification preferences for user: {}", request.getUserId());
        
        NotificationPreferences preferences = preferencesService.updatePreferences(request);
        return ResponseEntity.ok(preferences);
    }
}