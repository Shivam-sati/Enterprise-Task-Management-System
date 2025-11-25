package com.taskmanagement.task.exception;

public class SubtaskNotFoundException extends TaskManagementException {
    
    public SubtaskNotFoundException(String subtaskId) {
        super("Subtask not found with ID: " + subtaskId, "SUBTASK_NOT_FOUND");
    }
    
    public SubtaskNotFoundException(String subtaskId, String taskId) {
        super("Subtask not found with ID: " + subtaskId + " for task: " + taskId, "SUBTASK_NOT_FOUND");
    }
}