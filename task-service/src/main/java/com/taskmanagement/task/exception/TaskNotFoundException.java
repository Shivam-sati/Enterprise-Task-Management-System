package com.taskmanagement.task.exception;

public class TaskNotFoundException extends TaskManagementException {
    
    public TaskNotFoundException(String taskId) {
        super("Task not found with ID: " + taskId, "TASK_NOT_FOUND");
    }
    
    public TaskNotFoundException(String taskId, String userId) {
        super("Task not found with ID: " + taskId + " for user: " + userId, "TASK_NOT_FOUND");
    }
}