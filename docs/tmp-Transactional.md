# @Transactional 注解的工作原理

```text
一句话总结
@Transactional 是 Spring AOP + 动态代理 实现的声明式事务，本质就是在你的方法前后自动加事务控制代码：开启事务 → 执行方法 → 成功提交、异常回滚。
```

```text
一、核心工作流程


1. 项目启动 → Spring 扫描带有 @Transactional 的类/方法
2. 生成【动态代理对象】替代原对象
3. 调用方法时 → 先走代理对象的【事务拦截器】
4. 拦截器做：开启事务 → 执行目标方法 → 提交/回滚
```

```text
二、详细工作原理



1. 扫描与解析
Spring 启动时，通过 TransactionAnnotationParser 扫描类 / 方法上的 @Transactional
解析注解属性：
    propagation 传播机制
    isolation 隔离级别
    rollbackFor 回滚异常
    timeout 超时时间
把这些信息存起来，后面生成代理要用


2. 生成动态代理（核心！）
Spring 会给标了 @Transactional 的类 创建代理对象：
    JDK 动态代理（有接口）
    CGLIB 代理（无接口）
你调用的不是原类，而是代理类！


3. 执行时：事务拦截器干活
真正调用方法时，会进入 TransactionInterceptor 事务拦截器，执行固定逻辑：
try {
    // 1. 开启事务（根据传播行为决定）
    // 2. 获取数据库连接
    // 3. 关闭自动提交
    invoke 你的方法();
    // 4. 方法正常执行完 → 提交事务
    commit();
} catch (异常 e) {
    // 5. 异常 → 判断是否需要回滚
    rollback();
} finally {
    // 6. 恢复连接状态、清理资源
}


4. 回滚规则（默认行为）
遇到 RuntimeException / Error → 自动回滚
遇到 Checked Exception（编译时异常） → 不回滚
可以用 rollbackFor = Exception.class 强制所有异常都回滚
```

```text
三、最关键的底层组件



你只需要记 3 个：
    @Transactional：事务注解（标记）
    TransactionInterceptor：事务拦截器（干活的）
    PlatformTransactionManager：事务管理器（真正开启 / 提交 / 回滚）
```