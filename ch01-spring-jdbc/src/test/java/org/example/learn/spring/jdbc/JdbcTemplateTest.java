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

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JdbcConfig.class)
public class JdbcTemplateTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setup() {
        jdbcTemplate.execute("create table user(id INTEGER NOT NULL AUTO_INCREMENT, name VARCHAR(16), create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbcTemplate.update("insert into user(name) values (?)","zhangsan");
    }

    @Test
    public void testQuery() {
        Integer res = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        System.out.println("查询结果：" + res);
    }

    @Test
    public void testQueryObject() {
        List<User> user = jdbcTemplate.query("select * from user where name = ?", new BeanPropertyRowMapper<>(User.class), "zhangsan");
        System.out.println("user = " + user);
    }
}
