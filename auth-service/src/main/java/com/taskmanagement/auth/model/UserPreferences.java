package com.taskmanagement.auth.model;

public class UserPreferences {
    private NotificationPreferences notifications;
    private String timezone;
    private String language;
    
    public UserPreferences() {
        this.notifications = new NotificationPreferences();
        this.timezone = "UTC";
        this.language = "en";
    }
    
    // Getters and Setters
    public NotificationPreferences getNotifications() {
        return notifications;
    }
    
    public void setNotifications(NotificationPreferences notifications) {
        this.notifications = notifications;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public static class NotificationPreferences {
        private boolean email = true;
        private boolean sms = false;
        private boolean push = true;
        
        // Getters and Setters
        public boolean isEmail() {
            return email;
        }
        
        public void setEmail(boolean email) {
            this.email = email;
        }
        
        public boolean isSms() {
            return sms;
        }
        
        public void setSms(boolean sms) {
            this.sms = sms;
        }
        
        public boolean isPush() {
            return push;
        }
        
        public void setPush(boolean push) {
            this.push = push;
        }
    }
}