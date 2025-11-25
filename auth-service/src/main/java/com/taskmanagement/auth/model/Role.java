package com.taskmanagement.auth.model;

public enum Role {
    ADMIN("ADMIN"),
    USER("USER"),
    GUEST("GUEST");
    
    private final String value;
    
    Role(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}