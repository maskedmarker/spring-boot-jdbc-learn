package org.example.learn.spring.boot.jdbc.dao;

import org.example.learn.spring.boot.jdbc.model.User;

import java.util.List;

public interface UserDao {
    
    /**
     * 根据ID查询用户
     */
    User findById(Integer id);
    
    /**
     * 查询所有用户
     */
    List<User> findAll();
    
    /**
     * 根据名称查询用户
     */
    List<User> findByName(String name);
    
    /**
     * 插入用户
     */
    int insert(User user);
    
    /**
     * 更新用户
     */
    int update(User user);
    
    /**
     * 根据ID删除用户
     */
    int deleteById(Integer id);
    
    /**
     * 统计用户数量
     */
    int count();
}
