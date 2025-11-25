package com.taskmanagement.notification.listener;

import com.taskmanagement.notification.config.RabbitMQConfig;
import com.taskmanagement.notification.dto.NotificationRequest;
import com.taskmanagement.notification.dto.TaskEvent;
import com.taskmanagement.notification.model.NotificationChannel;
import com.taskmanagement.notification.model.NotificationType;
import com.taskmanagement.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventListener {
    
    private final NotificationService notificationService;
    
    @RabbitListener(queues = RabbitMQConfig.TASK_EVENTS_QUEUE)
    public void handleTaskEvent(TaskEvent taskEvent) {
        log.info("Received task event: {} for task: {}", taskEvent.getEventType(), taskEvent.getTaskId());
        
        try {
            NotificationRequest notificationRequest = createNotificationFromTaskEvent(taskEvent);
            if (notificationRequest != null) {
                notificationService.createNotification(notificationRequest);
            }
        } catch (Exception e) {
            log.error("Failed to process task event: {}", taskEvent.getTaskId(), e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
    
    private NotificationRequest createNotificationFromTaskEvent(TaskEvent taskEvent) {
        NotificationType notificationType = mapEventTypeToNotificationType(taskEvent.getEventType());
        if (notificationType == null) {
            log.warn("Unknown event type: {}", taskEvent.getEventType());
            return null;
        }
        
        String recipientUserId = determineRecipientUserId(taskEvent);
        if (recipientUserId == null) {
            log.warn("Could not determine recipient for task event: {}", taskEvent.getTaskId());
            return null;
        }
        
        Map<String, Object> templateData = createTemplateData(taskEvent);
        
        return NotificationRequest.builder()
                .userId(recipientUserId)
                .type(notificationType)
                .channels(getDefaultChannelsForType(notificationType))
                .subject(createSubject(taskEvent, notificationType))
                .message(createMessage(taskEvent, notificationType))
                .templateData(templateData)
                .relatedEntityId(taskEvent.getTaskId())
                .relatedEntityType("task")
                .build();
    }
    
    private NotificationType mapEventTypeToNotificationType(String eventType) {
        switch (eventType.toUpperCase()) {
            case "CREATED":
                return NotificationType.TASK_CREATED;
            case "UPDATED":
                return NotificationType.TASK_UPDATED;
            case "COMPLETED":
                return NotificationType.TASK_COMPLETED;
            case "ASSIGNED":
                return NotificationType.TASK_ASSIGNED;
            case "DUE_REMINDER":
                return NotificationType.TASK_DUE_REMINDER;
            case "OVERDUE":
                return NotificationType.TASK_OVERDUE;
            default:
                return null;
        }
    }
    
    private String determineRecipientUserId(TaskEvent taskEvent) {
        // For assigned tasks, notify the assignee
        if ("ASSIGNED".equals(taskEvent.getEventType()) && taskEvent.getAssignedUserId() != null) {
            return taskEvent.getAssignedUserId();
        }
        
        // For other events, notify the task owner
        return taskEvent.getUserId();
    }
    
    private Set<NotificationChannel> getDefaultChannelsForType(NotificationType type) {
        switch (type) {
            case TASK_DUE_REMINDER:
            case TASK_OVERDUE:
                return Set.of(NotificationChannel.EMAIL, NotificationChannel.PUSH);
            case TASK_ASSIGNED:
                return Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP);
            case TASK_CREATED:
            case TASK_UPDATED:
            case TASK_COMPLETED:
            default:
                return Set.of(NotificationChannel.IN_APP);
        }
    }
    
    private String createSubject(TaskEvent taskEvent, NotificationType type) {
        switch (type) {
            case TASK_CREATED:
                return "New Task Created: " + taskEvent.getTitle();
            case TASK_UPDATED:
                return "Task Updated: " + taskEvent.getTitle();
            case TASK_COMPLETED:
                return "Task Completed: " + taskEvent.getTitle();
            case TASK_ASSIGNED:
                return "Task Assigned to You: " + taskEvent.getTitle();
            case TASK_DUE_REMINDER:
                return "Task Due Soon: " + taskEvent.getTitle();
            case TASK_OVERDUE:
                return "Task Overdue: " + taskEvent.getTitle();
            default:
                return "Task Notification: " + taskEvent.getTitle();
        }
    }
    
    private String createMessage(TaskEvent taskEvent, NotificationType type) {
        StringBuilder message = new StringBuilder();
        
        switch (type) {
            case TASK_CREATED:
                message.append("A new task has been created: <strong>")
                       .append(taskEvent.getTitle())
                       .append("</strong>");
                break;
            case TASK_UPDATED:
                message.append("Task <strong>")
                       .append(taskEvent.getTitle())
                       .append("</strong> has been updated");
                break;
            case TASK_COMPLETED:
                message.append("Task <strong>")
                       .append(taskEvent.getTitle())
                       .append("</strong> has been completed");
                break;
            case TASK_ASSIGNED:
                message.append("You have been assigned to task: <strong>")
                       .append(taskEvent.getTitle())
                       .append("</strong>");
                break;
            case TASK_DUE_REMINDER:
                message.append("Task <strong>")
                       .append(taskEvent.getTitle())
                       .append("</strong> is due soon");
                break;
            case TASK_OVERDUE:
                message.append("Task <strong>")
                       .append(taskEvent.getTitle())
                       .append("</strong> is overdue");
                break;
            default:
                message.append("Task notification for: <strong>")
                       .append(taskEvent.getTitle())
                       .append("</strong>");
        }
        
        if (taskEvent.getDescription() != null && !taskEvent.getDescription().trim().isEmpty()) {
            message.append("<br><br>Description: ")
                   .append(taskEvent.getDescription());
        }
        
        if (taskEvent.getDueDate() != null) {
            message.append("<br>Due Date: ")
                   .append(taskEvent.getDueDate().toString());
        }
        
        if (taskEvent.getPriority() != null) {
            message.append("<br>Priority: ")
                   .append(taskEvent.getPriority());
        }
        
        return message.toString();
    }
    
    private Map<String, Object> createTemplateData(TaskEvent taskEvent) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskEvent.getTaskId());
        data.put("taskTitle", taskEvent.getTitle());
        data.put("taskDescription", taskEvent.getDescription());
        data.put("taskPriority", taskEvent.getPriority());
        data.put("taskStatus", taskEvent.getStatus());
        data.put("dueDate", taskEvent.getDueDate());
        data.put("eventType", taskEvent.getEventType());
        
        return data;
    }
}