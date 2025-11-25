package com.taskmanagement.task.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class RecurringPattern {
    
    @NotNull
    private RecurringType type;
    
    @Min(1)
    private int interval;
    
    private LocalDateTime endDate;
    
    private int maxOccurrences;
    
    // Constructors
    public RecurringPattern() {
        this.interval = 1;
    }
    
    public RecurringPattern(RecurringType type, int interval) {
        this.type = type;
        this.interval = interval;
    }
    
    public RecurringPattern(RecurringType type, int interval, LocalDateTime endDate) {
        this.type = type;
        this.interval = interval;
        this.endDate = endDate;
    }
    
    public RecurringPattern(RecurringType type, int interval, int maxOccurrences) {
        this.type = type;
        this.interval = interval;
        this.maxOccurrences = maxOccurrences;
    }
    
    // Getters and Setters
    public RecurringType getType() {
        return type;
    }
    
    public void setType(RecurringType type) {
        this.type = type;
    }
    
    public int getInterval() {
        return interval;
    }
    
    public void setInterval(int interval) {
        this.interval = interval;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public int getMaxOccurrences() {
        return maxOccurrences;
    }
    
    public void setMaxOccurrences(int maxOccurrences) {
        this.maxOccurrences = maxOccurrences;
    }
    
    // Helper methods
    public LocalDateTime calculateNextDueDate(LocalDateTime currentDueDate) {
        if (currentDueDate == null) {
            return null;
        }
        
        switch (type) {
            case DAILY:
                return currentDueDate.plusDays(interval);
            case WEEKLY:
                return currentDueDate.plusWeeks(interval);
            case MONTHLY:
                return currentDueDate.plusMonths(interval);
            case YEARLY:
                return currentDueDate.plusYears(interval);
            default:
                throw new IllegalStateException("Unknown recurring type: " + type);
        }
    }
    
    public boolean shouldCreateNextOccurrence(LocalDateTime currentDate, int currentOccurrenceCount) {
        // Check if we've reached the end date
        if (endDate != null && currentDate.isAfter(endDate)) {
            return false;
        }
        
        // Check if we've reached the maximum occurrences
        if (maxOccurrences > 0 && currentOccurrenceCount >= maxOccurrences) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return "RecurringPattern{" +
                "type=" + type +
                ", interval=" + interval +
                ", endDate=" + endDate +
                ", maxOccurrences=" + maxOccurrences +
                '}';
    }
}