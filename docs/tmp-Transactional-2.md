# @Transactional 注解的工作原理-启动

```text
整体链路



SpringBoot 启动
   ↓
自动配置类：TransactionAutoConfiguration
   ↓
注册 2 个核心Bean：
   1. TransactionInterceptor（事务拦截器）
   2. BeanFactoryTransactionAttributeSourceAdvisor（事务切面=切点+通知）
   ↓
AOP 代理创建：AnnotationAwareAspectJAutoProxyCreator
   ↓
代理对象判断：哪些类/方法带有 @Transactional
   ↓
方法调用时：
   代理 → TransactionInterceptor → 事务管理器 → 开启/提交/回滚
```


```text
@AutoConfigureAfter({ JtaAutoConfiguration.class, HibernateJpaAutoConfiguration.class, 
    DataSourceTransactionManagerAutoConfiguration.class,       // 需要依赖DataSourceTransactionManager(即PlatformTransactionManager)
    Neo4jDataAutoConfiguration.class })
public class TransactionAutoConfiguration {

    // 支持自定义修改PlatformTransactionManager
	@Bean
	@ConditionalOnMissingBean
	public TransactionManagerCustomizers platformTransactionManagerCustomizers(ObjectProvider<PlatformTransactionManagerCustomizer<?>> customizers) {
		return new TransactionManagerCustomizers(customizers.orderedStream().collect(Collectors.toList()));
	}

    // 支持reactive范式的ReactiveTransactionManager类型的TransactionManager
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnSingleCandidate(ReactiveTransactionManager.class)
	public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
		return TransactionalOperator.create(transactionManager);
	}

    // 支持传统imperative范式的PlatformTransactionManager类型的TransactionManager,
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(PlatformTransactionManager.class)
	public static class TransactionTemplateConfiguration {

        // 传统范式的PlatformTransactionManager需要TransactionTemplate
		@Bean
		@ConditionalOnMissingBean(TransactionOperations.class)
		public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
			return new TransactionTemplate(transactionManager);
		}

	}

    // 携带@Transactional的类需要被代理来通过AOP管理事务
    // 那么通过@EnableTransactionManagement的TransactionManagementConfigurationSelector引入了自动代理AutoProxyRegistrar和aop的事务管理ProxyTransactionManagementConfiguration
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(TransactionManager.class)
	@ConditionalOnMissingBean(AbstractTransactionManagementConfiguration.class)
	public static class EnableTransactionManagementConfiguration {

		@Configuration(proxyBeanMethods = false)
		@EnableTransactionManagement(proxyTargetClass = false)
		@ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "false",matchIfMissing = false)
		public static class JdkDynamicAutoProxyConfiguration {

		}

		@Configuration(proxyBeanMethods = false)
		@EnableTransactionManagement(proxyTargetClass = true)
		@ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "true", matchIfMissing = true)
		public static class CglibAutoProxyConfiguration {

		}

	}
}
```

```text
@Configuration class that registers the Spring infrastructure beans necessary to enable proxy-based annotation-driven transaction management.


@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {

	@Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor(TransactionAttributeSource transactionAttributeSource, TransactionInterceptor transactionInterceptor) {

		BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
		advisor.setTransactionAttributeSource(transactionAttributeSource);
		advisor.setAdvice(transactionInterceptor);
		if (this.enableTx != null) {
			advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
		}
		return advisor;
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionAttributeSource transactionAttributeSource() {
		return new AnnotationTransactionAttributeSource();
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionInterceptor transactionInterceptor(TransactionAttributeSource transactionAttributeSource) {
		TransactionInterceptor interceptor = new TransactionInterceptor();
		interceptor.setTransactionAttributeSource(transactionAttributeSource);
		if (this.txManager != null) {
			interceptor.setTransactionManager(this.txManager);
		}
		return interceptor;
	}

}
```


```text
二、逐步骤详细拆解


1. Spring Boot 启动：自动配置加载
核心类：TransactionAutoConfiguration
作用：Spring Boot 事务自动配置入口，是一切的起点。
    它是 spring-boot-autoconfigure 里的类
    当项目中存在 spring-tx（事务包）、spring-aop 时自动生效
    它会向 Spring 容器注册事务核心组件

2. 注册事务核心组件（TransactionAutoConfiguration 内部干的事）
（1）TransactionInterceptor —— 事务核心拦截器
    真正控制事务开启、提交、回滚的类
    实现了 MethodInterceptor（AOP 方法拦截器）
    持有 TransactionManager（事务管理器）
    内部逻辑：
    plaintext
    try {
        开启事务
        执行目标方法
        提交事务
    } catch (Exception e) {
        判断是否回滚
        回滚事务
    }
（2）TransactionAttributeSource
    作用：解析 @Transactional 注解
    读取注解里的：传播机制、隔离级别、回滚异常、超时等
    实现类：AnnotationTransactionAttributeSource
（3）BeanFactoryTransactionAttributeSourceAdvisor
    作用：事务切面 = 切点 + 通知
    Advisor = Pointcut（切点） + Advice（通知）
    切点：判断哪些方法需要被事务代理
    通知：就是上面的 TransactionInterceptor
    
    
3. AOP 核心：创建代理对象
    核心类：AnnotationAwareAspectJAutoProxyCreator
    作用：自动代理创建器，Spring AOP 核心
    继承 BeanPostProcessor
    在所有 Bean 初始化之后，判断：
        这个类 / 方法上有没有 @Transactional
        有没有匹配事务切面
    如果需要事务 → 生成 JDK 动态代理 / CGLIB 代理对象
    最终放回 Spring 容器的是代理对象，不是原生对象    


4. 切点匹配：哪些方法需要事务？
核心类：TransactionAttributeSourcePointcut
作用：匹配带有 @Transactional 注解的类或方法
    扫描方法 / 类上是否存在 @Transactional
    存在 → 匹配成功 → 走代理拦截
    不存在 → 跳过
```

```text
三、核心类总结




类名	                                                    作用
TransactionAutoConfiguration	                        Spring Boot 事务自动配置入口
TransactionInterceptor	                                事务拦截器，控制开启、提交、回滚
TransactionAttributeSource	                            解析 @Transactional 注解属性
BeanFactoryTransactionAttributeSourceAdvisor	        事务切面（切点 + 通知）
AnnotationAwareAspectJAutoProxyCreator	                AOP 自动代理创建器
TransactionAttributeSourcePointcut	                    切点：匹配 @Transactional 方法
PlatformTransactionManager	                            事务管理器，操作数据库连接
```