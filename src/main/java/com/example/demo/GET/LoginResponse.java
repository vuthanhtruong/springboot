package com.example.demo.GET;  // Ensure this is in the correct package (where your controller is)

public class LoginResponse {

    private boolean success;
    private String redirectUrl;

    // Constructor
    public LoginResponse(boolean success, String redirectUrl) {
        this.success = success;
        this.redirectUrl = redirectUrl;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}