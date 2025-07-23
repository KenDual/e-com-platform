package com.maiphuhai.repository;

import com.maiphuhai.model.Product;
import com.maiphuhai.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<User> USER_ROW_MAPPER = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();

            user.setUserId(rs.getInt("UserId"));
            user.setUsername(rs.getString("Username"));
            user.setPasswordHash(rs.getString("PasswordHash"));
            user.setRoleId(rs.getInt("RoleId"));
            user.setCreatedAt(rs.getTimestamp("CreatedAt"));

            return user;
        }
    };

    //Find all users
    public List<User> findAll() {
        String sql = "SELECT * FROM Users ORDER BY CreatedAt DESC";
        return jdbcTemplate.query(sql, USER_ROW_MAPPER);
    }

    //Find user by ID
    public User findById(int userId) {
        String sql = "SELECT * FROM Users WHERE UserId = ?";
        return jdbcTemplate.queryForObject(sql, USER_ROW_MAPPER, userId);
    }

    //Find user by username
    public User findByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE Username = ?";
        return jdbcTemplate.queryForObject(sql, USER_ROW_MAPPER, username);
    }

    //Find users by role
    public List<User> findByRoleId(int roleId) {
        String sql = "SELECT * FROM Users WHERE RoleId = ? ORDER BY CreatedAt DESC";
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, roleId);
    }

    //Create new user
    public User createUser(User user) {
        String sql = "INSERT INTO Users (Username, PasswordHash, RoleId) VALUES (?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setInt(3, user.getRoleId());
            return ps;
        }, keyHolder);

        user.setUserId(keyHolder.getKey().intValue());
        return findById(user.getUserId());
    }

    //Update user
    public boolean updateUser(User user) {
        String sql = "UPDATE Users SET Username = ?, PasswordHash = ?, RoleId = ? WHERE UserId = ?";
        int rowsAffected = jdbcTemplate.update(sql, user.getUsername(), user.getPasswordHash(), user.getRoleId(), user.getUserId());
        return rowsAffected > 0;
    }

    //Update password
    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE Users SET PasswordHash = ? WHERE UserId = ?";
        int rowsAffected = jdbcTemplate.update(sql, newPasswordHash, userId);
        return rowsAffected > 0;
    }

    //Delete user by ID
    public boolean deleteById(int userId) {
        String sql = "DELETE FROM Users WHERE UserId = ?";
        int rowsAffected = jdbcTemplate.update(sql, userId);
        return rowsAffected > 0;
    }

    //Check if username exists
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM Users WHERE Username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }
}
