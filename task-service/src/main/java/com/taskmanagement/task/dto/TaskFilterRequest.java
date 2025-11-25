package com.taskmanagement.task.dto;

import com.taskmanagement.task.model.TaskStatus;
import com.taskmanagement.task.model.TaskPriority;

import java.time.LocalDateTime;
import java.util.List;

public class TaskFilterRequest {
    
    private List<TaskStatus> statuses;
    private List<TaskPriority> priorities;
    private List<String> tags;
    private LocalDateTime dueDateFrom;
    private LocalDateTime dueDateTo;
    private Boolean isOverdue;
    private Boolean isRecurring;
    private String searchText;
    private String sortBy;
    private String sortDirection;
    private int page;
    private int size;
    
    // Constructors
    public TaskFilterRequest() {
        this.page = 0;
        this.size = 20;
        this.sortBy = "createdAt";
        this.sortDirection = "desc";
    }
    
    // Getters and Setters
    public List<TaskStatus> getStatuses() {
        return statuses;
    }
    
    public void setStatuses(List<TaskStatus> statuses) {
        this.statuses = statuses;
    }
    
    public List<TaskPriority> getPriorities() {
        return priorities;
    }
    
    public void setPriorities(List<TaskPriority> priorities) {
        this.priorities = priorities;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public LocalDateTime getDueDateFrom() {
        return dueDateFrom;
    }
    
    public void setDueDateFrom(LocalDateTime dueDateFrom) {
        this.dueDateFrom = dueDateFrom;
    }
    
    public LocalDateTime getDueDateTo() {
        return dueDateTo;
    }
    
    public void setDueDateTo(LocalDateTime dueDateTo) {
        this.dueDateTo = dueDateTo;
    }
    
    public Boolean getOverdue() {
        return isOverdue;
    }
    
    public void setOverdue(Boolean overdue) {
        isOverdue = overdue;
    }
    
    public Boolean getRecurring() {
        return isRecurring;
    }
    
    public void setRecurring(Boolean recurring) {
        isRecurring = recurring;
    }
    
    public String getSearchText() {
        return searchText;
    }
    
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    public String getSortDirection() {
        return sortDirection;
    }
    
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
}