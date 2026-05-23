# DataSourceUtils

spring通过DataSourceUtils将数据库事务/数据库连接池这些隐藏起来了.


```java
public static Connection doGetConnection(DataSource dataSource) throws SQLException {
    
    // 如果当前线程已经开启事务且已经设置好了connection,可以查询到正在使用的connection
    ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
    
    // 如果当前connection已经开启事务
    if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
        conHolder.requested();
        if (!conHolder.hasConnection()) {
            conHolder.setConnection(fetchConnection(dataSource));
        }
        return conHolder.getConnection();
    }
    // Else we either got no holder or an empty thread-bound holder here.

    // 如果当前线程还没有connection,用dataSource新建一个connection
    Connection con = fetchConnection(dataSource);

    // 如果当前线程开启了事务,
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        try {
            // Use same Connection for further JDBC actions within the transaction.
            // Thread-bound object will get removed by synchronization at transaction completion.
            ConnectionHolder holderToUse = conHolder;
            if (holderToUse == null) {
                holderToUse = new ConnectionHolder(con);
            }
            else {
                holderToUse.setConnection(con);
            }
            holderToUse.requested();
            
            // 注册事务各生命周期钩子方法(事务提交前/后/回滚前/后自动执行自定义业务逻辑)
            TransactionSynchronizationManager.registerSynchronization(new ConnectionSynchronization(holderToUse, dataSource));
            
            holderToUse.setSynchronizedWithTransaction(true);
            if (holderToUse != conHolder) {
                // 💥💥💥为当前线程绑定dataSource+connection
                TransactionSynchronizationManager.bindResource(dataSource, holderToUse);
            }
        }
        catch (RuntimeException ex) {
            // Unexpected exception from external delegation call -> close Connection and rethrow.
            releaseConnection(con, dataSource);
            throw ex;
        }
    }

    return con;
}
```

```java
public static void doReleaseConnection(@Nullable Connection con, @Nullable DataSource dataSource) throws SQLException {
    if (con == null) {
        return;
    }
    if (dataSource != null) {
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
        if (conHolder != null && connectionEquals(conHolder, con)) {
            // It's the transactional Connection: Don't close it.
            conHolder.released();
            return;
        }
    }
    doCloseConnection(con, dataSource);
}
```



## ConnectionSynchronization

定义事务各生命周期钩子方法(事务提交前/后/回滚前/后自动执行自定义业务逻辑)

```java
private static class ConnectionSynchronization extends TransactionSynchronizationAdapter {

    private final ConnectionHolder connectionHolder;

    private final DataSource dataSource;

    private int order;

    private boolean holderActive = true;

    public ConnectionSynchronization(ConnectionHolder connectionHolder, DataSource dataSource) {
        this.connectionHolder = connectionHolder;
        this.dataSource = dataSource;
        this.order = getConnectionSynchronizationOrder(dataSource);
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public void suspend() {
        if (this.holderActive) {
            TransactionSynchronizationManager.unbindResource(this.dataSource);
            if (this.connectionHolder.hasConnection() && !this.connectionHolder.isOpen()) {
                // Release Connection on suspend if the application doesn't keep
                // a handle to it anymore. We will fetch a fresh Connection if the
                // application accesses the ConnectionHolder again after resume,
                // assuming that it will participate in the same transaction.
                releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
                this.connectionHolder.setConnection(null);
            }
        }
    }

    @Override
    public void resume() {
        if (this.holderActive) {
            TransactionSynchronizationManager.bindResource(this.dataSource, this.connectionHolder);
        }
    }

    @Override
    public void beforeCompletion() {

        if (!this.connectionHolder.isOpen()) {
            TransactionSynchronizationManager.unbindResource(this.dataSource);
            this.holderActive = false;
            if (this.connectionHolder.hasConnection()) {
                releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
            }
        }
    }

    @Override
    public void afterCompletion(int status) {
        if (this.holderActive) {
            // The thread-bound ConnectionHolder might not be available anymore,
            // since afterCompletion might get called from a different thread.
            TransactionSynchronizationManager.unbindResourceIfPossible(this.dataSource);
            this.holderActive = false;
            if (this.connectionHolder.hasConnection()) {
                releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
                // Reset the ConnectionHolder: It might remain bound to the thread.
                this.connectionHolder.setConnection(null);
            }
        }
        this.connectionHolder.reset();
    }
}
```