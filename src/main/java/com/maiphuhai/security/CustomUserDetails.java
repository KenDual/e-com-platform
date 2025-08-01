package com.maiphuhai.security;

import com.maiphuhai.model.User;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

public class CustomUserDetails implements UserDetails {

    private final User user;
    public CustomUserDetails(User user) { this.user = user; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = (user.getRoleId() == 1) ? "ROLE_ADMIN" : "ROLE_CUSTOMER";
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getPassword()            { return user.getPasswordHash(); }
    @Override public String getUsername()            { return user.getEmail(); }   // hoặc Username tuỳ bạn
    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isAccountNonLocked()    { return user.isActive(); }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()             { return user.isActive(); }
}
