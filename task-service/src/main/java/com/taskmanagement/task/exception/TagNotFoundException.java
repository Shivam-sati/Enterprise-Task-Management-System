package com.taskmanagement.task.exception;

public class TagNotFoundException extends TaskManagementException {
    
    public TagNotFoundException(String tagId) {
        super("Tag not found with ID: " + tagId, "TAG_NOT_FOUND");
    }
    
    public TagNotFoundException(String tagName, String userId) {
        super("Tag not found with name: " + tagName + " for user: " + userId, "TAG_NOT_FOUND");
    }
}