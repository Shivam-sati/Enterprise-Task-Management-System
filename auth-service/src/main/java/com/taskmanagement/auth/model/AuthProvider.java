package com.taskmanagement.auth.model;

public enum AuthProvider {
    LOCAL("LOCAL"),
    GOOGLE("GOOGLE"),
    GITHUB("GITHUB");
    
    private final String value;
    
    AuthProvider(String value) {
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