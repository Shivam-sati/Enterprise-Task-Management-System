package com.taskmanagement.notification.service;

import com.taskmanagement.notification.dto.NotificationRequest;
import com.taskmanagement.notification.dto.NotificationResponse;
import com.taskmanagement.notification.model.*;
import com.taskmanagement.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private NotificationPreferencesService preferencesService;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private NotificationPublisher notificationPublisher;
    
    @InjectMocks
    private NotificationService notificationService;
    
    private NotificationRequest testRequest;
    private NotificationPreferences testPreferences;
    private Notification testNotification;
    
    @BeforeEach
    void setUp() {
        testRequest = NotificationRequest.builder()
                .userId("user123")
                .recipientEmail("test@example.com")
                .type(NotificationType.TASK_CREATED)
                .subject("Test Subject")
                .message("Test Message")
                .relatedEntityId("task123")
                .relatedEntityType("task")
                .build();
        
        testPreferences = NotificationPreferences.builder()
                .userId("user123")
                .emailEnabled(true)
                .inAppEnabled(true)
                .channelPreferences(Map.of(
                    NotificationType.TASK_CREATED, Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP)
                ))
                .build();
        
        testNotification = Notification.builder()
                .id("notif123")
                .userId("user123")
                .recipientEmail("test@example.com")
                .type(NotificationType.TASK_CREATED)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .subject("Test Subject")
                .message("Test Message")
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();
    }
    
    @Test
    void createNotification_ShouldCreateNotificationSuccessfully() {
        // Given
        when(preferencesService.getUserPreferences("user123")).thenReturn(testPreferences);
        when(notificationRepository.saveAll(anyList())).thenReturn(List.of(testNotification));
        
        // When
        NotificationResponse response = notificationService.createNotification(testRequest);
        
        // Then
        assertNotNull(response);
        assertEquals("notif123", response.getId());
        assertEquals("user123", response.getUserId());
        assertEquals(NotificationType.TASK_CREATED, response.getType());
        
        verify(preferencesService).getUserPreferences("user123");
        verify(notificationRepository).saveAll(anyList());
    }
    
    @Test
    void processNotification_EmailChannel_ShouldCallEmailService() throws Exception {
        // Given
        testNotification.setChannel(NotificationChannel.EMAIL);
        
        // When
        notificationService.processNotification(testNotification);
        
        // Then
        verify(emailService).sendEmail(testNotification);
    }
    
    @Test
    void processNotification_InAppChannel_ShouldPublishInAppNotification() {
        // Given
        testNotification.setChannel(NotificationChannel.IN_APP);
        
        // When
        notificationService.processNotification(testNotification);
        
        // Then
        verify(notificationPublisher).publishInAppNotification(testNotification);
    }
    
    @Test
    void processNotification_ShouldHandleEmailServiceException() throws Exception {
        // Given
        testNotification.setChannel(NotificationChannel.EMAIL);
        doThrow(new RuntimeException("Email service error")).when(emailService).sendEmail(any());
        when(notificationRepository.save(any())).thenReturn(testNotification);
        
        // When
        notificationService.processNotification(testNotification);
        
        // Then
        verify(notificationRepository, times(2)).save(any(Notification.class));
        // First call sets status to FAILED, second call schedules retry
    }
    
    @Test
    void getUnreadCount_ShouldReturnCorrectCount() {
        // Given
        when(notificationRepository.countByUserIdAndStatus("user123", NotificationStatus.SENT))
                .thenReturn(5L);
        
        // When
        long count = notificationService.getUnreadCount("user123");
        
        // Then
        assertEquals(5L, count);
        verify(notificationRepository).countByUserIdAndStatus("user123", NotificationStatus.SENT);
    }
}