package com.finflow.auth.dto;

public class AuthResponse {
    private String token;
    private String email;
    private String role;
    private Long userId;

    public AuthResponse(String token, String email, String role, Long userId) {
        this.token = token;
        this.email = email;
        this.role = role;
        this.userId = userId;
    }

    public String getToken() { return token; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Long getUserId() { return userId; }
}
