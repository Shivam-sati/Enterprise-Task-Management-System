package com.taskmanagement.notification.controller;

import com.taskmanagement.notification.dto.NotificationRequest;
import com.taskmanagement.notification.dto.NotificationResponse;
import com.taskmanagement.notification.service.EmailService;
import com.taskmanagement.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    private final EmailService emailService;
    
    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody NotificationRequest request) {
        log.info("Received request to send notification for user: {}", request.getUserId());
        
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @PathVariable String userId,
            Pageable pageable) {
        log.info("Fetching notifications for user: {}", userId);
        
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        log.info("Fetching notifications for entity: {} with ID: {}", entityType, entityId);
        
        List<NotificationResponse> notifications = notificationService.getNotificationsByEntity(entityId, entityType);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String userId) {
        log.info("Fetching unread count for user: {}", userId);
        
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
    
    @PostMapping("/test-email")
    public ResponseEntity<Map<String, String>> sendTestEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "Test Email") String subject,
            @RequestParam(defaultValue = "This is a test email from the Task Management System.") String message) {
        log.info("Sending test email to: {}", email);
        
        try {
            emailService.sendTestEmail(email, subject, message);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Test email sent successfully"));
        } catch (Exception e) {
            log.error("Failed to send test email", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Failed to send test email: " + e.getMessage()));
        }
    }
}