# JdbcTemplate

Spring在设计如何使用jdbc-api时,将需要执行的sql设计成callback

## 核心代码

spring在设计如何使用jdbc的api来执行sql的核心代码

```java
public <T> T execute(StatementCallback<T> action) throws DataAccessException {
    // 💥 从DataSource获取Connection(具体细节见DataSourceUtils)
    Connection con = DataSourceUtils.getConnection(obtainDataSource());
    Statement stmt = null;
    try {
        // 💥 用Connection创建Statement
        stmt = con.createStatement();
        // 💥 配置Statement
        applyStatementSettings(stmt);
        // 💥 执行用户sql及其参数
        T result = action.doInStatement(stmt);
        
        return result;
    }
    catch (SQLException ex) {
        // Release Connection early, to avoid potential connection pool deadlock in the case when the exception translator hasn't been initialized yet.
        String sql = getSql(action);
        // 💥 执行用户sql后要及时close Statement从而释放Statement相关的数据库系统资源(游标、内存资源)
        JdbcUtils.closeStatement(stmt);
        stmt = null;
        // 💥 "释放"占用的Connection(具体细节见DataSourceUtils)
        DataSourceUtils.releaseConnection(con, getDataSource());
        con = null;
        throw translateException("StatementCallback", sql, ex);
    }
    finally {
        JdbcUtils.closeStatement(stmt);
        DataSourceUtils.releaseConnection(con, getDataSource());
    }
}
```


```java
public static void closeStatement(@Nullable Statement stmt) {
    if (stmt != null) {
        try {
            // Calling the method close on a Statement object that is already closed has no effect.
            stmt.close();
        }
        catch (SQLException ex) {
            logger.trace("Could not close JDBC Statement", ex);
        }
        catch (Throwable ex) {
            // We don't trust the JDBC driver: It might throw RuntimeException or Error.
            logger.trace("Unexpected exception on closing JDBC Statement", ex);
        }
    }
}
```