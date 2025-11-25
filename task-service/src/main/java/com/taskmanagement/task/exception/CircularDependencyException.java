package com.taskmanagement.task.exception;

public class CircularDependencyException extends TaskManagementException {
    
    public CircularDependencyException(String taskId, String dependencyId) {
        super("Circular dependency detected: Task " + taskId + " cannot depend on " + dependencyId, "CIRCULAR_DEPENDENCY");
    }
    
    public CircularDependencyException(String message) {
        super(message, "CIRCULAR_DEPENDENCY");
    }
}