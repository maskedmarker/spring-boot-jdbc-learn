package org.example.learn.spring.boot.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TransactionTemplate 编程式事务管理测试类
 * 演示Spring中使用TransactionTemplate进行事务控制的核心功能
 */
@SpringBootTest
@ActiveProfiles("test")  // 使用test配置文件
class TransactionTemplateTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testTransactionTemplateExists() {
        // 验证TransactionTemplate已正确配置和注入
        assertNotNull(transactionTemplate);
        System.out.println("TransactionTemplate instance: " + transactionTemplate.getClass().getName());
    }

    @Test
    @Rollback(false)  // Whether the test-managed transaction should be rolled back after the test method has completed.
    void testTransactionCommit() {
        // 测试事务提交
        String countSql = "SELECT COUNT(*) FROM user";
        int countBefore = jdbcTemplate.queryForObject(countSql, Integer.class);

        // 使用TransactionTemplate执行事务
        Integer result = transactionTemplate.execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                // 插入一条记录
                jdbcTemplate.update("INSERT INTO user (name, email) VALUES (?, ?)", "tx_commit_test", "tx_commit@test.com");
                return jdbcTemplate.queryForObject(countSql, Integer.class);
            }
        });

        // 验证事务已提交，数据已插入
        assertNotNull(result);
        assertEquals(countBefore + 1, result);

        // 验证数据持久化
        int countAfter = jdbcTemplate.queryForObject(countSql, Integer.class);
        assertEquals(countBefore + 1, countAfter);

        System.out.println("Transaction commit successful, count changed from " + countBefore + " to " + countAfter);
    }

    @Test
    void testTransactionRollback() {
        // 测试事务回滚（抛出异常时）
        String countSql = "SELECT COUNT(*) FROM user";
        int countBefore = jdbcTemplate.queryForObject(countSql, Integer.class);

        try {
            transactionTemplate.execute(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction(TransactionStatus status) {
                    // 插入一条记录
                    jdbcTemplate.update("INSERT INTO user (name, email) VALUES (?, ?)", "tx_rollback_test", "tx_rollback@test.com");

                    // 模拟业务异常，触发回滚
                    throw new RuntimeException("Test exception to trigger rollback");
                }
            });
        } catch (RuntimeException e) {
            // 预期会抛出异常
            System.out.println("Expected exception caught: " + e.getMessage());
        }

        // 验证事务已回滚，数据未插入
        int countAfter = jdbcTemplate.queryForObject(countSql, Integer.class);
        assertEquals(countBefore, countAfter);

        System.out.println("Transaction rollback successful, count remains: " + countAfter);
    }

    @Test
    @Rollback(false)
    void testTransactionCallbackWithoutResult() {
        // 测试使用TransactionCallbackWithoutResult（无返回值）
        String countSql = "SELECT COUNT(*) FROM user";
        int countBefore = jdbcTemplate.queryForObject(countSql, Integer.class);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jdbcTemplate.update("INSERT INTO user (name, email) VALUES (?, ?)", "tx_no_result", "tx_no_result@test.com");
            }
        });

        // 验证事务已提交
        int countAfter = jdbcTemplate.queryForObject(countSql, Integer.class);
        assertEquals(countBefore + 1, countAfter);

        System.out.println("TransactionCallbackWithoutResult test successful");
    }

    @Test
    void testTransactionReadOnly() {
        // 测试只读事务
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setReadOnly(true);
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

        TransactionTemplate readOnlyTemplate = new TransactionTemplate(transactionTemplate.getTransactionManager(), def);

        Boolean result = readOnlyTemplate.execute(status -> {
            // 在只读事务中查询
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
            return count != null && count > 0;
        });

        assertTrue(result);
        System.out.println("Read-only transaction test successful");
    }

    @Test
    void testTransactionStatusMethods() {
        // 测试事务状态方法
        transactionTemplate.execute(status -> {
            // 验证当前事务状态 - 这是一个新事务
            assertTrue(status.isNewTransaction());
            
            // 验证事务没有被标记为回滚
            assertFalse(status.isRollbackOnly());
            
            // 查询用户数量
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
            assertNotNull(count);
            assertTrue(count >= 0);
            
            return null;
        });

        System.out.println("Transaction status methods test successful");
    }

    @Test
    void testTransactionManualRollback() {
        // 测试手动回滚
        String countSql = "SELECT COUNT(*) FROM user";
        int countBefore = jdbcTemplate.queryForObject(countSql, Integer.class);

        transactionTemplate.execute(status -> {
            // 插入一条记录
            jdbcTemplate.update("INSERT INTO user (name, email) VALUES (?, ?)", "tx_manual_rb", "tx_manual_rb@test.com");

            // 手动标记回滚
            status.setRollbackOnly();
            return null;
        });

        // 验证事务已回滚
        int countAfter = jdbcTemplate.queryForObject(countSql, Integer.class);
        assertEquals(countBefore, countAfter);

        System.out.println("Manual rollback test successful");
    }

    @Test
    @Rollback(false)
    void testMultipleOperationsInTransaction() {
        // 测试事务中的多个操作
        String countSql = "SELECT COUNT(*) FROM user";
        int countBefore = jdbcTemplate.queryForObject(countSql, Integer.class);

        transactionTemplate.execute(status -> {
            // 执行多个操作
            jdbcTemplate.update("INSERT INTO user (name, email) VALUES (?, ?)", "tx_multi_1", "tx_multi_1@test.com");
            jdbcTemplate.update("INSERT INTO user (name, email) VALUES (?, ?)", "tx_multi_2", "tx_multi_2@test.com");
            jdbcTemplate.update("UPDATE user SET name = ? WHERE id = ?", "updated_by_tx", 1);

            return null;
        });

        // 验证所有操作都已提交
        int countAfter = jdbcTemplate.queryForObject(countSql, Integer.class);
        assertEquals(countBefore + 2, countAfter);

        // 验证更新操作
        String updatedName = jdbcTemplate.queryForObject("SELECT name FROM user WHERE id = ?", String.class, 1);
        assertEquals("updated_by_tx", updatedName);

        System.out.println("Multiple operations in transaction test successful");
    }

    @Test
    void testTransactionWithExceptionHandling() {
        // 测试事务中的异常处理
        String countSql = "SELECT COUNT(*) FROM user";
        int countBefore = jdbcTemplate.queryForObject(countSql, Integer.class);

        Boolean success = transactionTemplate.execute(status -> {
            try {
                jdbcTemplate.update("INSERT INTO user (name, email) VALUES (?, ?)", "tx_exc_test", "tx_exc_test@test.com");
                
                // 模拟业务校验失败
                throw new IllegalArgumentException("Validation failed");
            } catch (IllegalArgumentException e) {
                // 标记回滚
                status.setRollbackOnly();
                System.out.println("Caught validation error, rolling back: " + e.getMessage());
                return false;
            }
        });

        assertFalse(success);
        int countAfter = jdbcTemplate.queryForObject(countSql, Integer.class);
        assertEquals(countBefore, countAfter);

        System.out.println("Transaction exception handling test successful");
    }
}
