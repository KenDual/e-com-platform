package com.maiphuhai.service;

import com.maiphuhai.model.User;
import com.maiphuhai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private PasswordEncoder encoder;

    /* ─────────── ĐĂNG KÝ ─────────── */
    @Transactional
    public User register(String username, String email, String rawPwd) {
        if (repo.existsByUsernameOrEmail(username, email))
            throw new IllegalStateException("Username hoặc email đã tồn tại");

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPwd));
        u.setRoleId(2);     // CUSTOMER

        int id = repo.insert(u);
        return repo.findById(id);
    }

    /* ─────────── XÁC THỰC (login thủ công) ─────────── */
    public User authenticate(String usernameOrEmail, String rawPwd) {
        User u;
        try {
            u = repo.findByUsernameOrEmail(usernameOrEmail);
        } catch (Exception e) {
            return null;
        }
        if (!u.isActive() || u.isDeleted()) return null;
        return encoder.matches(rawPwd, u.getPasswordHash()) ? u : null;
    }

    /* ─────────── ĐỔI MẬT KHẨU ─────────── */
    @Transactional
    public void changePassword(int uid, String rawPwd) {
        repo.updatePassword(uid, encoder.encode(rawPwd));
    }

    /* ─────────── ADMIN / TIỆN ÍCH ─────────── */
    public List<User> getPage(int p, int s, Boolean act) {
        return repo.findPage(p, s, act);
    }

    public int count(Boolean act) {
        return repo.count(act);
    }

    @Transactional
    public void setActive(int id, boolean a) {
        repo.setActive(id, a);
    }

    @Transactional
    public void softDelete(int id) {
        repo.softDelete(id);
    }

    @Transactional
    public void restore(int id) {
        repo.restore(id);
    }

    public User findByUsernameOrEmail(String v) {
        return repo.findByUsernameOrEmail(v);
    }

    // Search for user account
    @Transactional(readOnly = true)
    public List<User> search(String q, String by) {
        // Nếu không nhập gì, trả danh sách như cũ
        if (q == null || q.isBlank()) return repo.findAll();

        by = (by == null || by.isBlank()) ? "both" : by.toLowerCase();

        try {
            switch (by) {
                case "username" -> {
                    var u = repo.findByUsername(q);
                    return (u != null) ? List.of(u) : List.of();
                }
                case "email" -> {
                    var u = repo.findByEmail(q);
                    return (u != null) ? List.of(u) : List.of();
                }
                default -> { // both
                    // Username/Email đều UNIQUE => tối đa 1 kết quả
                    var u = repo.findByUsernameOrEmail(q);
                    return (u != null) ? List.of(u) : List.of();
                }
            }
        } catch (Exception ignore) {
            return List.of(); // không tìm thấy -> trả list rỗng (tránh ném exception ra view)
        }
    }


    public List<User> findAll() {
        return repo.findAll();
    }

    public User update(User u) {
        int rows = repo.update(u);
        if (rows == 0) throw new IllegalStateException("User not found or deleted");
        return repo.findById(u.getUserId());
    }

    public User findById(int id) {
        try {
            return repo.findById(id);
        } catch (Exception e) {
            return null;
        }
    }
}
