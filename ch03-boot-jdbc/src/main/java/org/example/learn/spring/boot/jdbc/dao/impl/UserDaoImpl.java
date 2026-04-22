package org.example.learn.spring.boot.jdbc.dao.impl;

import org.example.learn.spring.boot.jdbc.dao.UserDao;
import org.example.learn.spring.boot.jdbc.model.User;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class UserDaoImpl implements UserDao {

    private final JdbcTemplate jdbcTemplate;

    public UserDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    @Override
    public User findById(Integer id) {
        String sql = "SELECT id, name, email, create_time, update_time FROM user WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), id);
        return users.isEmpty() ? null : users.get(0);
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, name, email, create_time, update_time FROM user ORDER BY id";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class));
    }

    @Override
    public List<User> findByName(String name) {
        String sql = "SELECT id, name, email, create_time, update_time FROM user WHERE name = ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), name);
    }

    @Override
    public int insert(User user) {
        String sql = "INSERT INTO user (name, email) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            return ps;
        }, keyHolder);
        
        // 从KeyHolder中获取生成的ID，使用getKeyList获取第一个键
        if (keyHolder.getKeyList() != null && !keyHolder.getKeyList().isEmpty()) {
            Object keyObj = keyHolder.getKeyList().get(0).get("ID");
            if (keyObj instanceof Number) {
                user.setId(((Number) keyObj).intValue());
            }
        }
        return 1;
    }

    @Override
    public int update(User user) {
        String sql = "UPDATE user SET name = ?, email = ? WHERE id = ?";
        return jdbcTemplate.update(sql, user.getName(), user.getEmail(), user.getId());
    }

    @Override
    public int deleteById(Integer id) {
        String sql = "DELETE FROM user WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM user";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
