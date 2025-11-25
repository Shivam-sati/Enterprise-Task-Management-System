package com.taskmanagement.task.model;

public enum SubtaskStatus {
    TODO("To Do"),
    COMPLETED("Completed");
    
    private final String displayName;
    
    SubtaskStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isCompleted() {
        return this == COMPLETED;
    }
}