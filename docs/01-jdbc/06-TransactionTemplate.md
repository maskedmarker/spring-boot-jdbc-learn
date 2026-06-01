# TransactionTemplate

```text
TransactionTemplate.execute方法只暴漏了一个入参TransactionStatus，是为让spring统一管理事务。
TransactionStatus只让应用代码标识当前事务是否要回滚（默认提交）/创建销毁savePoint/回滚到指定savePoint，不允许应用代码commit事务。
```

```java
public <T> T execute(TransactionCallback<T> action) throws TransactionException {
    Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");

    // ...
    TransactionStatus status = this.transactionManager.getTransaction(this);
    T result;
    try {
        result = action.doInTransaction(status);
    }
    catch (RuntimeException | Error ex) {
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
```
