package com.taskmanagement.task.service;

import com.taskmanagement.task.model.Task;
import com.taskmanagement.task.model.TaskStatus;
import com.taskmanagement.task.repository.TaskRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RecurringTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecurringTaskService.class);
    
    private final TaskRepository taskRepository;
    
    @Autowired
    public RecurringTaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }
    
    /**
     * Scheduled method to process recurring tasks
     * Runs every hour to check for completed recurring tasks that need new instances
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    public void processRecurringTasks() {
        logger.info("Processing recurring tasks...");
        
        LocalDateTime now = LocalDateTime.now();
        List<Task> completedRecurringTasks = taskRepository.findCompletedRecurringTasksBeforeDate(now);
        
        int processedCount = 0;
        for (Task task : completedRecurringTasks) {
            try {
                if (shouldCreateNextOccurrence(task, now)) {
                    createNextOccurrence(task);
                    processedCount++;
                }
            } catch (Exception e) {
                logger.error("Error processing recurring task: {}", task.getTaskId(), e);
            }
        }
        
        logger.info("Processed {} recurring tasks", processedCount);
    }
    
    /**
     * Creates the next occurrence of a recurring task
     */
    public Task createNextOccurrence(Task originalTask) {
        logger.info("Creating next occurrence for recurring task: {}", originalTask.getTaskId());
        
        if (!originalTask.isRecurring() || originalTask.getRecurringPattern() == null) {
            logger.warn("Task is not recurring or has no pattern: {}", originalTask.getTaskId());
            return null;
        }
        
        // Calculate next due date
        LocalDateTime nextDueDate = originalTask.getRecurringPattern()
                .calculateNextDueDate(originalTask.getDueDate());
        
        if (nextDueDate == null) {
            logger.warn("Could not calculate next due date for task: {}", originalTask.getTaskId());
            return null;
        }
        
        // Create new task instance
        Task nextTask = createTaskCopy(originalTask);
        nextTask.setDueDate(nextDueDate);
        nextTask.setStatus(TaskStatus.TODO);
        nextTask.setCompletedAt(null);
        nextTask.setActualHours(null);
        
        Task savedTask = taskRepository.save(nextTask);
        
        logger.info("Created next occurrence: {} with due date: {}", 
                   savedTask.getTaskId(), nextDueDate);
        
        return savedTask;
    }
    
    /**
     * Manually creates the next occurrence of a recurring task
     */
    public Task createNextOccurrenceManually(String taskId, String userId) {
        logger.info("Manually creating next occurrence for task: {} by user: {}", taskId, userId);
        
        Task task = taskRepository.findByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        
        if (!task.isRecurring()) {
            throw new IllegalArgumentException("Task is not recurring: " + taskId);
        }
        
        return createNextOccurrence(task);
    }
    
    /**
     * Updates the recurring pattern for a task
     */
    public void updateRecurringPattern(Task task) {
        logger.info("Updating recurring pattern for task: {}", task.getTaskId());
        
        if (!task.isRecurring()) {
            // If task is no longer recurring, we don't need to do anything special
            return;
        }
        
        if (task.getRecurringPattern() == null) {
            logger.warn("Recurring task has no pattern: {}", task.getTaskId());
            task.setRecurring(false);
        }
        
        taskRepository.save(task);
    }
    
    /**
     * Stops a recurring task (marks it as non-recurring)
     */
    public void stopRecurring(String taskId, String userId) {
        logger.info("Stopping recurring for task: {} by user: {}", taskId, userId);
        
        Task task = taskRepository.findByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        
        task.setRecurring(false);
        task.setRecurringPattern(null);
        
        taskRepository.save(task);
        
        logger.info("Stopped recurring for task: {}", taskId);
    }
    
    /**
     * Gets all recurring tasks for a user
     */
    public List<Task> getRecurringTasks(String userId) {
        logger.debug("Getting recurring tasks for user: {}", userId);
        return taskRepository.findByUserIdAndIsRecurringTrue(userId);
    }
    
    private boolean shouldCreateNextOccurrence(Task task, LocalDateTime currentTime) {
        if (task.getRecurringPattern() == null) {
            return false;
        }
        
        // Check if we should create the next occurrence based on the pattern
        return task.getRecurringPattern().shouldCreateNextOccurrence(currentTime, 0);
    }
    
    private Task createTaskCopy(Task originalTask) {
        Task copy = new Task();
        
        // Copy basic properties
        copy.setUserId(originalTask.getUserId());
        copy.setTitle(originalTask.getTitle());
        copy.setDescription(originalTask.getDescription());
        copy.setPriority(originalTask.getPriority());
        copy.setTags(originalTask.getTags() != null ? 
                    List.copyOf(originalTask.getTags()) : null);
        copy.setEstimatedHours(originalTask.getEstimatedHours());
        copy.setDependencies(originalTask.getDependencies() != null ? 
                           List.copyOf(originalTask.getDependencies()) : null);
        
        // Copy recurring properties
        copy.setRecurring(originalTask.isRecurring());
        copy.setRecurringPattern(originalTask.getRecurringPattern());
        
        return copy;
    }
}