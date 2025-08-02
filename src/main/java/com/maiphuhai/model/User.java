package com.maiphuhai.model;

import jakarta.persistence.*;

import java.sql.Timestamp;

//@Entity
//@Table(name = "users")
public class User {

//    @Id
//    @GeneratedValue

    private int     userId;
    private String  username;
    private String  email;
    private String  passwordHash;
    private int     roleId;
    private boolean isActive;
    private boolean isDeleted;
    private Timestamp deletedAt;
    private Timestamp createdAt;

    public User() {
    }

    public User(int userId, String username, String email, String passwordHash, int roleId, boolean isActive, boolean isDeleted, Timestamp deletedAt, Timestamp createdAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roleId = roleId;
        this.isActive = isActive;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Timestamp getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Timestamp deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
