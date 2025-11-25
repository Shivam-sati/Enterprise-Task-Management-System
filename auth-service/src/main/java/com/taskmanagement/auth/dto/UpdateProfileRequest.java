package com.taskmanagement.auth.dto;

import com.taskmanagement.auth.model.UserPreferences;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    private UserPreferences preferences;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String name, UserPreferences preferences) {
        this.name = name;
        this.preferences = preferences;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(UserPreferences preferences) {
        this.preferences = preferences;
    }
}