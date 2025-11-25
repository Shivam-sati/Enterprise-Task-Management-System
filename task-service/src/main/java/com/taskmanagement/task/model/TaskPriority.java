package com.taskmanagement.task.model;

public enum TaskPriority {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    CRITICAL(4, "Critical");
    
    private final int level;
    private final String displayName;
    
    TaskPriority(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isHigherThan(TaskPriority other) {
        return this.level > other.level;
    }
    
    public boolean isLowerThan(TaskPriority other) {
        return this.level < other.level;
    }
    
    public static TaskPriority fromLevel(int level) {
        for (TaskPriority priority : values()) {
            if (priority.level == level) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Invalid priority level: " + level);
    }
}