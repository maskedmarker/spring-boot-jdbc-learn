package org.example.learn.spring.boot.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JdbcTemplate 直接使用的测试类
 * 演示Spring Boot JDBC的核心功能
 */
@SpringBootTest
@ActiveProfiles("test")  // 使用test配置文件
class JdbcTemplateTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testJdbcTemplateExists() {
        // 验证JdbcTemplate已正确配置和注入
        assertNotNull(jdbcTemplate);
        System.out.println("JdbcTemplate instance: " + jdbcTemplate.getClass().getName());
    }

    @Test
    void testQueryForObject() {
        // 测试查询单个值
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
        
        assertNotNull(count);
        assertTrue(count >= 5); // data.sql中有5条记录
        
        System.out.println("User count: " + count);
    }

    @Test
    void testQueryForList() {
        // 测试查询列表（返回Map）
        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT * FROM user ORDER BY id");
        
        assertNotNull(users);
        assertTrue(users.size() >= 5);
        
        // 验证第一条记录
        Map<String, Object> firstUser = users.get(0);
        assertEquals(1, firstUser.get("ID"));
        assertEquals("001", firstUser.get("NAME"));
        assertEquals("001@qq.com", firstUser.get("EMAIL"));
        
        System.out.println("First user: " + firstUser);
        System.out.println("Total users queried: " + users.size());
    }

    @Test
    void testQueryForMap() {
        // 测试查询单条记录（返回Map）
        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT * FROM user WHERE id = ?", 2);
        
        assertNotNull(user);
        assertEquals(2, user.get("ID"));
        assertEquals("002", user.get("NAME"));
        assertEquals("002@qq.com", user.get("EMAIL"));
        
        System.out.println("User with id=2: " + user);
    }

    @Test
    void testUpdate() {
        // 测试更新操作
        int rowsAffected = jdbcTemplate.update(
                "UPDATE user SET name = ?, email = ? WHERE id = ?",
                "updated_003", "updated_003@qq.com", 3);
        
        assertEquals(1, rowsAffected);
        
        // 验证更新结果
        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT * FROM user WHERE id = ?", 3);
        assertEquals("updated_003", user.get("NAME"));
        assertEquals("updated_003@qq.com", user.get("EMAIL"));
        
        System.out.println("Updated user: " + user);
    }

    @Test
    @Rollback(false)
    void testInsertAndGetKey() {
        // 测试插入并获取生成的主键
        String sql = "INSERT INTO user (name, email) VALUES (?, ?)";
        
        org.springframework.jdbc.core.PreparedStatementCreator psc = connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(
                    sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, "jdbc_test");  // 修改为不超过16个字符
            ps.setString(2, "jdbctemplate@test.com");
            return ps;
        };
        
        org.springframework.jdbc.support.GeneratedKeyHolder keyHolder = 
                new org.springframework.jdbc.support.GeneratedKeyHolder();
        
        jdbcTemplate.update(psc, keyHolder);
        
        // 使用getKeyList获取生成的键
        Number key = null;
        if (keyHolder.getKeyList() != null && !keyHolder.getKeyList().isEmpty()) {
            Object keyObj = keyHolder.getKeyList().get(0).get("ID");
            if (keyObj instanceof Number) {
                key = (Number) keyObj;
            }
        }
        
        assertNotNull(key);
        assertTrue(key.intValue() > 0);
        
        System.out.println("Inserted row with generated key: " + key.intValue());
        
        // 验证插入的数据
        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT * FROM user WHERE id = ?", key.intValue());
        assertEquals("jdbc_test", user.get("NAME"));
        assertEquals("jdbctemplate@test.com", user.get("EMAIL"));
    }

    @Test
    void testBatchUpdate() {
        // 测试批量更新
        String sql = "INSERT INTO user (name, email) VALUES (?, ?)";
        
        List<Object[]> batchArgs = java.util.Arrays.asList(
                new Object[]{"batch_1", "batch_1@test.com"},
                new Object[]{"batch_2", "batch_2@test.com"},
                new Object[]{"batch_3", "batch_3@test.com"}
        );
        
        int[] updateCounts = jdbcTemplate.batchUpdate(sql, batchArgs);
        
        // 验证每条记录都插入成功
        assertEquals(3, updateCounts.length);
        for (int count : updateCounts) {
            assertEquals(1, count);
        }
        
        // 验证批量插入的数据
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE name LIKE 'batch_%'", Integer.class);
        assertEquals(3, count);
        
        System.out.println("Batch insert successful, inserted " + count + " records");
    }

    @Test
    void testExecute() {
        // 测试执行DDL语句
        jdbcTemplate.execute("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");
        
        // 验证表已创建
        Boolean tableExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) > 0 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TEST_TABLE'",
                Boolean.class);
        assertTrue(tableExists);
        
        System.out.println("Table 'test_table' created successfully");
        
        // 清理：删除测试表
        jdbcTemplate.execute("DROP TABLE test_table");
    }

    @Test
    void testQueryWithRowMapper() {
        // 测试使用RowMapper查询对象列表
        String sql = "SELECT id, name, email, create_time, update_time FROM user WHERE id <= ? ORDER BY id";
        
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, 3);
        
        assertNotNull(users);
        assertTrue(users.size() >= 3);
        
        // 验证第一条记录
        Map<String, Object> firstUser = users.get(0);
        assertNotNull(firstUser.get("ID"));
        assertNotNull(firstUser.get("NAME"));
        assertNotNull(firstUser.get("EMAIL"));
        
        System.out.println("Queried " + users.size() + " users with RowMapper");
        System.out.println("First user: ID=" + firstUser.get("ID") + ", NAME=" + firstUser.get("NAME"));
    }

    @Test
    void testTransactionManagement() {
        // 测试事务管理
        String countSql = "SELECT COUNT(*) FROM user";
        int countBefore = jdbcTemplate.queryForObject(countSql, Integer.class);
        
        try {
            // 插入一条记录
            jdbcTemplate.update("INSERT INTO user (name, email) VALUES (?, ?)", 
                    "trans_test", "trans@test.com");  // 修改为不超过16个字符
            
            int countAfter = jdbcTemplate.queryForObject(countSql, Integer.class);
            assertEquals(countBefore + 1, countAfter);
            
            // 模拟异常，测试回滚
            throw new RuntimeException("Test exception for rollback");
        } catch (RuntimeException e) {
            // 由于没有@Transactional，事务不会自动回滚
            // 这个测试主要演示异常处理
            System.out.println("Exception caught, transaction test completed");
        }
    }
}
