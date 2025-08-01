package com.maiphuhai.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRegistration {
    @NotBlank
    @Size(min=4, max=50)
    private String username;

    @Email
    @NotBlank
    private String email;

    @NotBlank @Size(min=8)
    private String password;

    public UserRegistration() {}

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
