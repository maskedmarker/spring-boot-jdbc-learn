# TransactionTemplate

```text
TransactionTemplate.execute方法只暴漏了一个入参TransactionStatus，是为让spring统一管理事务,防止应用代码私自提交事务.
TransactionStatus只让应用代码标识当前事务是否要回滚/创建销毁savePoint/回滚到指定savePoint,不允许应用代码commit事务(没有要求就按默认处理方式-提交事务).
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

```text
外层的事务获得的 TransactionStatus 对象和内层事务获得的 TransactionStatus 对象是同一个对象吗？

分两种传播行为，结果完全不一样：
1. 内层是 NESTED 嵌套事务（共用同一 MySQL 连接）
    外层和内层拿到的 TransactionStatus 是【不同对象】，但绑定【同一个物理事务、同一个 ThreadLocal 资源】
2. 内层是 REQUIRES_NEW（新开独立 MySQL 连接）
    外层、内层 TransactionStatus 完全是两个独立对象，各自对应独立事务、独立连接
    
    
    
一、先看 NESTED 场景（最关键，你前面一直在聊）
1. 执行链路
外层 TransactionTemplate.execute()
    txManager.getTransaction() → 创建 外层 status1
    存入 ThreadLocal：resources 里是同一个 Connection
内层（NESTED）再次调用 execute()
    事务管理器发现：当前已有活跃事务
    不会新建 Connection，不会新开物理事务
    只会创建一个新的 TransactionStatus 对象 status2
    在 status2 内部记录：基于外层事务创建了一个 Savepoint
2. 关键点
对象：status1 != status2（两个 Java 对象）
底层 MySQL：同一个 Connection、同一个物理事务
ThreadLocal 里的 resources、synchronizations 都是同一套
内层回滚：只回滚到 Savepoint，不结束外层事务
内层不能 commit，只能外层最终 commit    
```


```text
事务传播级别

传播行为	                行为
NEVER                   已有事务则抛 IllegalTransactionStateException
NOT_SUPPORTED           挂起当前事务，返回无物理事务的 TransactionStatus（newTransaction=false）
REQUIRES_NEW            挂起旧事务 → startTransaction 开新事务
NESTED                  若允许嵌套：优先用 Savepoint；否则再 startTransaction
REQUIRED / SUPPORTS     加入现有事务（newTransaction=false），可选校验隔离级别/只读


同样都是不需要事务,NEVER检测到处于事务环境中则抛出异常,而NOT_SUPPORTED则挂起已有的事务,不在事务环境中执行.
同样都是需要事务,REQUIRED检测到处于事务环境中则加入同一个事务,没有则创建新事务;NESTED类似于REQUIRED(优先用Savepoint实现新事务);REQUIRES_NEW检测到处于事务环境中则挂起已有的事务,创建新事务.
```


## commit

```text
processCommit 做四件事：

1. 按顺序触发事务同步回调（beforeCommit → beforeCompletion → 物理提交 → afterCommit → afterCompletion）
2. 根据 TransactionStatus 决定：释放 savepoint、调用 doCommit，或仅做同步/检查
3. 处理 commit 过程中的各类异常（含 UnexpectedRollbackException）
4. 在 finally 里必定执行 cleanupAfterCompletion（标记完成、清理 ThreadLocal、恢复被挂起的外层事务）



典型场景速查

外层 @Transactional + 内层 @Transactional(REQUIRED)
内层 commit()：newTransaction=false → 不 doCommit，只跑同步回调；外层 commit() 时才真正 doCommit。

@Transactional(PROPAGATION_NESTED)
内层 commit()：hasSavepoint=true → 释放 savepoint；内层失败则 rollbackToHeldSavepoint() 回到外层 savepoint。

@Transactional(PROPAGATION_REQUIRES_NEW)
内层 newTransaction=true → 内层 doCommit/doRollback；cleanupAfterCompletion 里 resume 恢复外层。

TransactionTemplate.execute()
正常结束调用 commit(status) → 最终进入 processCommit；业务异常则走 rollback() → processRollback，不会进 processCommit。
```

```text
private void processCommit(DefaultTransactionStatus status) throws TransactionException {
    try {
        boolean beforeCompletionInvoked = false;

        try {
            boolean unexpectedRollback = false;
            prepareForCommit(status);
            triggerBeforeCommit(status);
            triggerBeforeCompletion(status);
            beforeCompletionInvoked = true;

            // PROPAGATION_NESTED 嵌套事务
            // 释放 savepoint，不调用 doCommit；外层事务继续
            if (status.hasSavepoint()) {
                unexpectedRollback = status.isGlobalRollbackOnly();
                status.releaseHeldSavepoint();
            }
            
            // 本次 getTransaction 新开了物理事务
            // 调用 doCommit(status)（如 JDBC connection.commit()）
            else if (status.isNewTransaction()) {
                unexpectedRollback = status.isGlobalRollbackOnly();
                doCommit(status);
            }
            
            // 参与外层事务（REQUIRED/SUPPORTS 等）
            // 不提交物理事务；若 failEarlyOnGlobalRollbackOnly=true 则检查全局 rollback-only
            else if (isFailEarlyOnGlobalRollbackOnly()) {
                unexpectedRollback = status.isGlobalRollbackOnly();
            }

            if (unexpectedRollback) {
                throw new UnexpectedRollbackException(
                        "Transaction silently rolled back because it has been marked as rollback-only");
            }
        }
        catch (UnexpectedRollbackException ex) {
            triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
            throw ex;
        }
        catch (TransactionException ex) {
            if (isRollbackOnCommitFailure()) {
                doRollbackOnCommitException(status, ex);
            }
            else {
                triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
            }
            throw ex;
        }
        catch (RuntimeException | Error ex) {
            if (!beforeCompletionInvoked) {
                triggerBeforeCompletion(status);
            }
            doRollbackOnCommitException(status, ex);
            throw ex;
        }


        try {
            triggerAfterCommit(status);
        }
        finally {
            triggerAfterCompletion(status, TransactionSynchronization.STATUS_COMMITTED);
        }

    }
    finally {
        cleanupAfterCompletion(status);
    }
}
```