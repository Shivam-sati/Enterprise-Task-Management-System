package com.taskmanagement.notification.service;

import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.taskmanagement.notification.model.Notification;
import com.taskmanagement.notification.model.NotificationChannel;
import com.taskmanagement.notification.model.NotificationStatus;
import com.taskmanagement.notification.model.NotificationType;
import com.taskmanagement.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    
    @Mock
    private SendGrid sendGrid;
    
    @Mock
    private NotificationTemplateRepository templateRepository;
    
    @InjectMocks
    private EmailService emailService;
    
    private Notification testNotification;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@taskmanagement.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Task Management System");
        
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
                .build();
    }
    
    @Test
    void sendEmail_ShouldSendSuccessfully() throws IOException {
        // Given
        Response mockResponse = new Response();
        mockResponse.setStatusCode(202);
        when(sendGrid.api(any())).thenReturn(mockResponse);
        
        // When & Then
        assertDoesNotThrow(() -> emailService.sendEmail(testNotification));
        verify(sendGrid).api(any());
    }
    
    @Test
    void sendEmail_ShouldThrowExceptionOnFailure() throws IOException {
        // Given
        Response mockResponse = new Response();
        mockResponse.setStatusCode(400);
        mockResponse.setBody("Bad Request");
        when(sendGrid.api(any())).thenReturn(mockResponse);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> emailService.sendEmail(testNotification));
        assertTrue(exception.getMessage().contains("Failed to send email"));
        verify(sendGrid).api(any());
    }
    
    @Test
    void sendTestEmail_ShouldSendSuccessfully() throws IOException {
        // Given
        Response mockResponse = new Response();
        mockResponse.setStatusCode(202);
        when(sendGrid.api(any())).thenReturn(mockResponse);
        
        // When & Then
        assertDoesNotThrow(() -> emailService.sendTestEmail(
                "test@example.com", 
                "Test Subject", 
                "Test Message"
        ));
        verify(sendGrid).api(any());
    }
}