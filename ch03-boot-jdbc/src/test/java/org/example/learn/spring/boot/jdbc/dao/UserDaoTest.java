package org.example.learn.spring.boot.jdbc.dao;

import org.example.learn.spring.boot.jdbc.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserDao 单元测试类
 * 测试Spring Boot JDBC的完整功能
 */
@SpringBootTest
@ActiveProfiles("test")  // 使用test配置文件
@Transactional  // 每个测试方法在事务中运行，测试后自动回滚
class UserDaoTest {

    @Autowired
    private UserDao userDao;

    @Test
    void testFindAll() {
        // 测试查询所有用户
        List<User> users = userDao.findAll();
        
        // 验证结果不为空
        assertNotNull(users);
        // data.sql中插入了5条记录
        assertTrue(users.size() >= 5);
        
        // 打印所有用户
        users.forEach(user -> System.out.println("User: " + user));
    }

    @Test
    void testFindById() {
        // 测试根据ID查询用户
        User user = userDao.findById(1);
        
        // 验证用户存在
        assertNotNull(user);
        assertEquals(1, user.getId());
        assertNotNull(user.getName());
        assertNotNull(user.getEmail());
        assertNotNull(user.getCreateTime());
        assertNotNull(user.getUpdateTime());
        
        System.out.println("Found user by ID 1: " + user);
    }

    @Test
    void testFindById_NotFound() {
        // 测试查询不存在的用户
        User user = userDao.findById(99999);
        
        // 验证返回null
        assertNull(user);
    }

    @Test
    void testFindByName() {
        // 测试根据名称查询用户
        List<User> users = userDao.findByName("001");
        
        // 验证结果
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("001", users.get(0).getName());
        assertEquals("001@qq.com", users.get(0).getEmail());
        
        System.out.println("Found user by name '001': " + users.get(0));
    }

    @Test
    void testFindByName_NotFound() {
        // 测试查询不存在的名称
        List<User> users = userDao.findByName("nonexistent");
        
        // 验证返回空列表
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    @Rollback(false)  // 这个测试不回滚，演示插入操作
    void testInsert() {
        // 测试插入新用户
        User newUser = new User("test_user", "test@example.com");
        
        int result = userDao.insert(newUser);
        
        // 验证插入成功
        assertEquals(1, result);
        // 验证生成了ID
        assertNotNull(newUser.getId());
        assertTrue(newUser.getId() > 0);
        
        System.out.println("Inserted user with ID: " + newUser.getId());
        
        // 验证可以查询到新插入的用户
        User insertedUser = userDao.findById(newUser.getId());
        assertNotNull(insertedUser);
        assertEquals("test_user", insertedUser.getName());
        assertEquals("test@example.com", insertedUser.getEmail());
    }

    @Test
    void testUpdate() {
        // 先查询一个用户
        User user = userDao.findById(1);
        assertNotNull(user);
        
        String originalName = user.getName();
        String originalEmail = user.getEmail();
        
        // 修改用户信息
        user.setName("updated_name");
        user.setEmail("updated@example.com");
        
        // 执行更新
        int result = userDao.update(user);
        
        // 验证更新成功
        assertEquals(1, result);
        
        // 验证更新后的数据
        User updatedUser = userDao.findById(1);
        assertNotNull(updatedUser);
        assertEquals("updated_name", updatedUser.getName());
        assertEquals("updated@example.com", updatedUser.getEmail());
        
        System.out.println("Updated user: " + updatedUser);
        
        // 恢复原始数据（因为@Transactional，测试结束后会自动回滚）
        user.setName(originalName);
        user.setEmail(originalEmail);
        userDao.update(user);
    }

    @Test
    void testDeleteById() {
        // 先统计当前用户数量
        int countBefore = userDao.count();
        
        // 创建一个临时用户用于删除
        User tempUser = new User("temp_user", "temp@example.com");
        userDao.insert(tempUser);
        
        // 验证用户已创建
        int countAfterInsert = userDao.count();
        assertEquals(countBefore + 1, countAfterInsert);
        
        // 删除该用户
        int result = userDao.deleteById(tempUser.getId());
        
        // 验证删除成功
        assertEquals(1, result);
        
        // 验证用户已被删除
        User deletedUser = userDao.findById(tempUser.getId());
        assertNull(deletedUser);
        
        // 验证用户数量恢复
        int countAfterDelete = userDao.count();
        assertEquals(countBefore, countAfterDelete);
        
        System.out.println("Successfully deleted user with ID: " + tempUser.getId());
    }

    @Test
    void testCount() {
        // 测试统计用户数量
        int count = userDao.count();
        
        // 验证数量正确（data.sql中有5条记录）
        assertTrue(count >= 5);
        
        System.out.println("Total user count: " + count);
    }

    @Test
    void testTransactionRollback() {
        // 测试事务回滚
        int countBefore = userDao.count();
        
        // 插入一个新用户
        User newUser = new User("rollback_test", "rollback@test.com");
        userDao.insert(newUser);
        
        // 验证插入成功
        User insertedUser = userDao.findById(newUser.getId());
        assertNotNull(insertedUser);
        
        // 由于类上有@Transactional注解，测试方法结束后会自动回滚
        // 这里可以验证在当前事务中数据是存在的
        assertEquals(countBefore + 1, userDao.count());
        
        System.out.println("Transaction rollback test passed. Count before: " + countBefore + 
                          ", Count in transaction: " + userDao.count());
    }

    @Test
    void testMultipleOperations() {
        // 测试多个JDBC操作的组合
        
        // 1. 查询所有用户
        List<User> allUsers = userDao.findAll();
        assertNotNull(allUsers);
        int initialCount = allUsers.size();
        
        // 2. 插入新用户
        User user1 = new User("multi_test_1", "multi1@test.com");
        userDao.insert(user1);
        
        User user2 = new User("multi_test_2", "multi2@test.com");
        userDao.insert(user2);
        
        // 3. 验证数量增加
        assertEquals(initialCount + 2, userDao.count());
        
        // 4. 更新一个用户
        user1.setName("multi_test_1_upd");  // 修改为不超过16个字符
        userDao.update(user1);
        
        User updatedUser1 = userDao.findById(user1.getId());
        assertEquals("multi_test_1_upd", updatedUser1.getName());
        
        // 5. 删除一个用户
        userDao.deleteById(user2.getId());
        assertEquals(initialCount + 1, userDao.count());
        
        // 6. 按名称查询
        List<User> foundUsers = userDao.findByName("multi_test_1_upd");
        assertEquals(1, foundUsers.size());
        
        System.out.println("Multiple operations test passed successfully");
    }
}
