package com.maiphuhai.service;

import com.maiphuhai.model.User;
import com.maiphuhai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    // ──────────────── READ ────────────────
    public List<User> findAll()               { return userRepository.findAll(); }
    public User findById(int id)              { return userRepository.findById(id); }
    public User findByUsername(String u)      { return userRepository.findByUsername(u); }
    public List<User> findByRoleId(int roleId){ return userRepository.findByRoleId(roleId); }

    // ──────────────── WRITE ────────────────
    @Transactional
    public User save(User user) {               // dùng cho cả create & update
        if (user.getUserId() == 0) return userRepository.createUser(user);
        userRepository.updateUser(user);
        return findById(user.getUserId());
    }

    @Transactional
    public void deleteById(int id) { userRepository.deleteById(id); }

    @Transactional
    public void updatePassword(int id, String hash) {
        userRepository.updatePassword(id, hash);
    }

    public boolean existsByUsername(String u) { return userRepository.existsByUsername(u); }
}
