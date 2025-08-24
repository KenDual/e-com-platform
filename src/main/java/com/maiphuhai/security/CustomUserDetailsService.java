package com.maiphuhai.security;

import com.maiphuhai.model.User;
import com.maiphuhai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String login)
            throws UsernameNotFoundException {

        User u = userRepo.findByUsernameOrEmail(login);
        if (u == null) throw new UsernameNotFoundException("User not found");
        return new CustomUserDetails(u);
    }
}
