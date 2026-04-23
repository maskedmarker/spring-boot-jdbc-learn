

## 异常时回滚

Java中的异常层级
Throwable
    Error                -> 调用方未知的 
    RuntimeException     -> 调用方未知的
    Exception            -> 调用方已知的且可能会发生的异常状况

Error和RuntimeException被事务管理模块认作是异常情况发生了,需要回滚事务.
Exception被事务管理模块认为调用方会正确处理,事务管理模块无需回滚.

```text
public <T> T execute(TransactionCallback<T> action) throws TransactionException {
    Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");

    if (this.transactionManager instanceof CallbackPreferringPlatformTransactionManager) {
        return ((CallbackPreferringPlatformTransactionManager) this.transactionManager).execute(this, action);
    }
    else {
        TransactionStatus status = this.transactionManager.getTransaction(this);
        T result;
        try {
            result = action.doInTransaction(status);
        } 
        catch (RuntimeException | Error ex) {                                      // 💥💥💥 默认遇到unchecked-exception时回滚. checked-exception指的是Exception的子类
            // Transactional code threw application exception -> rollback
            rollbackOnException(status, ex);
            throw ex;
        }
        catch (Throwable ex) {
            // Transactional code threw unexpected exception -> rollback
            rollbackOnException(status, ex);
            throw new UndeclaredThrowableException(ex, "TransactionCallback threw undeclared checked exception");
        }
        this.transactionManager.commit(status);
        return result;
    }
}
```