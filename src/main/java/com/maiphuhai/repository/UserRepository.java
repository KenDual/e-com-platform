package com.maiphuhai.repository;

import com.maiphuhai.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class UserRepository {

    @Autowired private JdbcTemplate jdbc;

    /* ---------- RowMapper ---------- */
    private static final RowMapper<User> M = (rs, n) -> {
        User u = new User();
        u.setUserId     (rs.getInt   ("UserId"));
        u.setUsername   (rs.getString("Username"));
        u.setEmail      (rs.getString("Email"));
        u.setPasswordHash(rs.getString("PasswordHash"));
        u.setRoleId     (rs.getInt   ("RoleId"));
        u.setActive     (rs.getBoolean("IsActive"));
        u.setDeleted  (rs.getBoolean("IsDeleted"));
        u.setDeletedAt  (rs.getTimestamp("DeletedAt"));
        u.setCreatedAt  (rs.getTimestamp("CreatedAt"));
        return u;
    };

    /* ---------- BASIC GET ---------- */
    public List<User> findAll() {
        return jdbc.query("""
            SELECT * FROM Users
            WHERE IsDeleted = 0
            ORDER BY CreatedAt DESC
            """, M);
    }

    public User findById(int id){
        return jdbc.queryForObject("""
            SELECT * FROM Users WHERE UserId = ? AND IsDeleted = 0
            """, M, id);
    }

    public User findByUsername(String u){
        return jdbc.queryForObject("""
            SELECT * FROM Users WHERE Username = ? AND IsDeleted = 0
            """, M, u);
    }

    /* ---------- CHECK EXISTS ---------- */
    public boolean existsByUsernameOrEmail(String u,String e){
        Integer c = jdbc.queryForObject("""
            SELECT COUNT(*) FROM Users
            WHERE (Username = ? OR Email = ?) AND IsDeleted = 0
            """, Integer.class, u, e);
        return c!=null && c>0;
    }

    /* ---------- CREATE ---------- */
    public int insert(User u){
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                INSERT INTO Users
                (Username,Email,PasswordHash,RoleId)
                VALUES (?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,u.getUsername());
            ps.setString(2,u.getEmail());
            ps.setString(3,u.getPasswordHash());
            ps.setInt   (4,u.getRoleId());
            return ps;
        }, kh);
        return kh.getKey().intValue();
    }

    /* ---------- UPDATE PROFILE ---------- */
    public int update(User u){
        return jdbc.update("""
            UPDATE Users SET Username=?,Email=?,RoleId=?
            WHERE UserId=? AND IsDeleted=0
            """, u.getUsername(), u.getEmail(), u.getRoleId(), u.getUserId());
    }

    /* ---------- PASSWORD ---------- */
    public int updatePassword(int id,String hash){
        return jdbc.update("""
            UPDATE Users SET PasswordHash = ?
            WHERE UserId = ? AND IsDeleted = 0
            """, hash, id);
    }

    /* ---------- SOFT DELETE / RESTORE ---------- */
    public int softDelete(int id){
        return jdbc.update("""
            UPDATE Users SET IsDeleted=1, DeletedAt=SYSUTCDATETIME()
            WHERE UserId=? AND IsDeleted=0
            """, id);
    }
    public int restore(int id){
        return jdbc.update("""
            UPDATE Users SET IsDeleted=0, DeletedAt=NULL
            WHERE UserId=? AND IsDeleted=1
            """, id);
    }

    /* ---------- ACTIVATE / DEACTIVATE ---------- */
    public int setActive(int id, boolean a){
        return jdbc.update("""
            UPDATE Users SET IsActive = ?
            WHERE UserId = ? AND IsDeleted = 0
            """, a, id);
    }

    /* ---------- PAGINATION ---------- */
    public List<User> findPage(int page,int size, Boolean active){
        int offset = page * size;
        return jdbc.query("""
            SELECT * FROM Users
            WHERE IsDeleted = 0
              AND (? IS NULL OR IsActive = ?)
            ORDER BY CreatedAt DESC
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """, M, active, active, offset, size);
    }
    public int count(Boolean active){
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM Users
            WHERE IsDeleted = 0
              AND (? IS NULL OR IsActive = ?)
            """, Integer.class, active, active);
    }

    /* ---------- PURGE HARD DELETE (cron) ---------- */
    public int purgeDeletedOlderThan(int days){
        return jdbc.update("""
            DELETE FROM Users
            WHERE IsDeleted = 1
              AND DeletedAt < DATEADD(day, -?, SYSUTCDATETIME())
            """, days);
    }

    public User findByEmail(String e){
        return jdbc.queryForObject("""
        SELECT * FROM Users
        WHERE Email = ? AND IsDeleted = 0
        """, M, e);
    }

    /* ---------- FIND BY USERNAME *HOẶC* EMAIL ---------- */
    public User findByUsernameOrEmail(String v){
        return jdbc.queryForObject("""
        SELECT * FROM Users
        WHERE (Username = ? OR Email = ?) AND IsDeleted = 0
        """, M, v, v);
    }
}
