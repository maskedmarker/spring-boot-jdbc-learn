# TransactionSynchronizationManager

由于


```java
public abstract class TransactionSynchronizationManager {

    // 存放当前线程事务绑定的资源，核心存DataSource→ConnectionHolder连接资源
    private static final ThreadLocal<Map<Object, Object>> resources = new NamedThreadLocal<>("Transactional resources");
    
    // 存储当前线程注册的事务生命周期回调钩子, 事务提交前/后、回滚前/后自动执行自定义业务逻辑
    private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations = new NamedThreadLocal<>("Transaction synchronizations");

    // 记录当前事务名称，用于日志追踪、事务标识区分
    private static final ThreadLocal<String> currentTransactionName = new NamedThreadLocal<>("Current transaction name");

    // 标记当前事务是否为只读事务, 只读模式会优化数据库查询、禁止写操作，提升性能
    private static final ThreadLocal<Boolean> currentTransactionReadOnly = new NamedThreadLocal<>("Current transaction read-only status");

    // 保存当前事务隔离级别（读未提交 / 读已提交 / 可重复读 / 串行化）
    private static final ThreadLocal<Integer> currentTransactionIsolationLevel = new NamedThreadLocal<>("Current transaction isolation level");

    
    // 标记当前线程是否存在真实活跃事务, 用来判断是否处于事务内，决定连接复用、提交回滚逻辑
    private static final ThreadLocal<Boolean> actualTransactionActive = new NamedThreadLocal<>("Actual transaction active");
}    
```

## getResource

如果当前线程已经开启事务,则查找该事务相关的资源的中是否有所需的资源

映射关系
DataSource -> ConnectionHolder

```java
public static Object getResource(Object key) {
    // 如果key是代理对象,要找到最底层的被代理对象
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    Object value = doGetResource(actualKey);
    return value;
}


@Nullable
private static Object doGetResource(Object actualKey) {
    // 数据库事务相关的资源都放在threadLocal中,保证同一个线程
    Map<Object, Object> map = resources.get();
    if (map == null) {
        return null;
    }
    Object value = map.get(actualKey);
    // Transparently remove ResourceHolder that was marked as void...
    if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
        map.remove(actualKey);
        // Remove entire ThreadLocal if empty...
        if (map.isEmpty()) {
            resources.remove();
        }
        value = null;
    }
    return value;
}

// 💥💥💥绑定的资源在主动释放前,不允许通过绑定替换
public static void bindResource(Object key, Object value) throws IllegalStateException {
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);

    Map<Object, Object> map = resources.get();
    if (map == null) {
        map = new HashMap<>();
        resources.set(map);
    }
    Object oldValue = map.put(actualKey, value);
    // Transparently suppress a ResourceHolder that was marked as void...
    if (oldValue instanceof ResourceHolder && ((ResourceHolder) oldValue).isVoid()) {
        oldValue = null;
    }
    
    // 💥绑定的资源在主动释放前,不允许通过绑定替换
    if (oldValue != null) {
        throw new IllegalStateException("Already value [" + oldValue + "] for key [" + actualKey + "] bound to thread [" + Thread.currentThread().getName() + "]");
    }
    if (logger.isTraceEnabled()) {
        logger.trace("Bound value [" + value + "] for key [" + actualKey + "] to thread [" + Thread.currentThread().getName() + "]");
    }
}

public static Object unbindResource(Object key) throws IllegalStateException {
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    Object value = doUnbindResource(actualKey);
    if (value == null) {
        throw new IllegalStateException("No value for key [" + actualKey + "] bound to thread [" + Thread.currentThread().getName() + "]");
    }
    return value;
}

private static Object doUnbindResource(Object actualKey) {
    Map<Object, Object> map = resources.get();
    if (map == null) {
        return null;
    }
    Object value = map.remove(actualKey);
    // Remove entire ThreadLocal if empty...
    if (map.isEmpty()) {
        resources.remove();
    }
    // Transparently suppress a ResourceHolder that was marked as void...
    if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
        value = null;
    }
    if (value != null && logger.isTraceEnabled()) {
        logger.trace("Removed value [" + value + "] for key [" + actualKey + "] from thread [" + Thread.currentThread().getName() + "]");
    }
    return value;
}
```