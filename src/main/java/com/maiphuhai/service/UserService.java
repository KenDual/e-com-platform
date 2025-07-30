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

    @Autowired private UserRepository repo;
    @Autowired private BCryptPasswordEncoder encoder;

    /* ─────────── ĐĂNG KÝ ─────────── */
    @Transactional
    public User register(String username, String email, String rawPwd) {
        if ( repo.existsByUsernameOrEmail(username, email) )
            throw new IllegalStateException("Username hoặc email đã tồn tại");

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash( encoder.encode(rawPwd) );
        u.setRoleId(2);     // CUSTOMER

        int id = repo.insert(u);
        return repo.findById(id);
    }

    /* ─────────── XÁC THỰC (login thủ công) ─────────── */
    public User authenticate(String usernameOrEmail, String rawPwd) {
        User u;
        try {
            u = repo.findByUsernameOrEmail(usernameOrEmail);
        } catch (Exception e) {                       // không tìm thấy
            return null;
        }
        if (!u.isActive() || u.isDeleted()) return null;
        return encoder.matches(rawPwd, u.getPasswordHash()) ? u : null;
    }

    /* ─────────── ĐỔI MẬT KHẨU ─────────── */
    @Transactional
    public void changePassword(int uid, String rawPwd){
        repo.updatePassword(uid, encoder.encode(rawPwd));
    }

    /* ─────────── ADMIN / TIỆN ÍCH ─────────── */
    public List<User> getPage(int p,int s,Boolean act){ return repo.findPage(p,s,act); }
    public int  count(Boolean act){ return repo.count(act); }

    @Transactional public void setActive(int id, boolean a){ repo.setActive(id,a); }
    @Transactional public void softDelete(int id){ repo.softDelete(id); }
    @Transactional public void restore(int id){ repo.restore(id); }
}
