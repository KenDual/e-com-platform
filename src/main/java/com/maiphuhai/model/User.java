package com.maiphuhai.model;

import java.sql.Timestamp;

public class User {
    private int UserId;
    private String Username;
    private String email;
    private String PasswordHash;
    private int RoleId;
    private Timestamp CreatedAt;

    public User() {
    }

    public User(int userId, String username, String email, String passwordHash, int roleId, Timestamp createdAt) {
        UserId = userId;
        Username = username;
        this.email = email;
        PasswordHash = passwordHash;
        RoleId = roleId;
        CreatedAt = createdAt;
    }

    public int getUserId() {
        return UserId;
    }

    public void setUserId(int userId) {
        UserId = userId;
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return PasswordHash;
    }

    public void setPasswordHash(String passwordHash) {
        PasswordHash = passwordHash;
    }

    public int getRoleId() {
        return RoleId;
    }

    public void setRoleId(int roleId) {
        RoleId = roleId;
    }

    public Timestamp getCreatedAt() {
        return CreatedAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        CreatedAt = createdAt;
    }
}
