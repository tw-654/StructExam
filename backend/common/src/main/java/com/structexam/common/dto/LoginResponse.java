package com.structexam.common.dto;

public class LoginResponse {
    private Long id;
    private String token;
    private String username;
    private String realName;
    private String role;
    private Long expireTime;

    public LoginResponse() {
    }

    public LoginResponse(Long id, String token, String username, String realName, String role, Long expireTime) {
        this.id = id;
        this.token = token;
        this.username = username;
        this.realName = realName;
        this.role = role;
        this.expireTime = expireTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }
}
