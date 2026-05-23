# JDBC 原生的能力

```text
JDBC 原生的能力决定了spring如何设计框架.



以MySQL举例

1. 同一 Connection 同一时刻，MySQL 只允许存在一个活跃物理事务
        不存在数据库层面父子事务、多层独立事务，所有操作都归属这一个事务。
2. 原生支持 Savepoint 保存点机制
        在同一个大事务内部，可以标记多个点位，实现局部回滚：
            设置保存点：标记事务可以回滚到哪一步
            回滚到保存点：只撤销该点位之后的所有 SQL 修改，点位之前的数据保留不变
            全程不会结束整个大事务，依旧可以继续执行业务，最后统一整体 commit/rollback
3. 行为边界
        可以回滚局部、不支持局部单独提交
        事务内任意位置调用commit，直接终结整个事务，所有保存点全部失效
        
备注:
事务在提交前,事务内SQL都是串行执行,并写入事务缓冲区.
```



```text
💥💥💥 正是由于jdbc的Savepoint保存点只能串行使用,Spring决定了通过ThreadLocal来实现事务机制.



Spring的事务传播机制
Propagation.NESTED           嵌套事务,底层就是纯靠JDBC+保存点实现的逻辑嵌套
Propagation.REQUIRES_NEW     新事务, 直接用DataSource再新建一个Connection
```