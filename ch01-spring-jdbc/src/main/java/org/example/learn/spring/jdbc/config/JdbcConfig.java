package org.example.learn.spring.jdbc.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;

@Configuration
@PropertySource("classpath:db.properties")
public class JdbcConfig {

    @Resource
    private Environment env;

    @Bean
    public DataSource dataSource(){
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env.getProperty("db.jdbc.url"));
        config.setDriverClassName(env.getProperty("db.driver.class"));
        config.setUsername(env.getProperty("db.username"));
        config.setPassword(env.getProperty("db.password"));
        config.setMaximumPoolSize(Integer.parseInt(env.getProperty("hikari.max.pool.size")));
        config.setMinimumIdle(Integer.parseInt(env.getProperty("hikari.min.idle")));
        config.setIdleTimeout(Long.parseLong(env.getProperty("hikari.idle.timeout")));

        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource){
        return new JdbcTemplate(dataSource);
    }

    // 事务管理器
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource){
        return new DataSourceTransactionManager(dataSource);
    }

    // 编程式事务模板
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager){
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        // 默认传播级别、超时可按需设置
        template.setTimeout(30);
        return template;
    }
}
