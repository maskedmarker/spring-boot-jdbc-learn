package org.example.learn.spring.jdbc;

import org.example.learn.spring.jdbc.config.JdbcConfig;
import org.example.learn.spring.jdbc.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JdbcConfig.class)
public class TransactionTemplateTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Before
    public void setup() {
        jdbcTemplate.execute("create table user(id INTEGER NOT NULL AUTO_INCREMENT, name VARCHAR(16), create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
    }

    @Test
    public void testTxOk(){
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                // 数据库操作
                jdbcTemplate.update("insert into user(name) values (?)","zhangsan");
                // 抛出异常自动回滚
                // int i = 1/0;
            }
        });

        Integer id = jdbcTemplate.queryForObject("select id from user where name = 'zhangsan'", Integer.class);
        System.out.println("id = " + id);
    }

    @Test
    public void testTxFail(){
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    // 数据库操作
                    jdbcTemplate.update("insert into user(name) values (?)","zhangsan");
                    // 抛出异常自动回滚
                    int i = 1/0;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<User> user = jdbcTemplate.query("select * from user where name = ?", new BeanPropertyRowMapper<>(User.class), "zhangsan");
        System.out.println("user = " + user);
    }
}
