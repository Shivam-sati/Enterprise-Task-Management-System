package com.taskmanagement.auth.dto;

public class GenericApiResponse {
    private boolean success;
    private String message;

    public GenericApiResponse() {}

    public GenericApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}