# 事务的AOP

```text
AnnotationTransactionAttributeSource
    基于TransactionAnnotationParser,判断一个class是否携带了@Transactional,如果是则为代理的candidate
    基于TransactionAnnotationParser,从一个method上提取@Transactional注解信息给调用方(比如TransactionInterceptor.invoke)
 
SpringTransactionAnnotationParser是TransactionAnnotationParser的实现
    1. 判断class-level或者method-level是否携带@Transactional注解
    2. 解析@Transactional注解信息,用TransactionAttribute来容纳这些信息
```

```text
TransactionInterceptor作为Advice(MethodInterceptor是Advice的子类),在AOP中提供切面逻辑,开启事务/在事务执行sql/提交或回滚事务

TransactionInterceptor因为需要方法层面的@Transactional注解信息,所以需要依赖AnnotationTransactionAttributeSource
```

```text
BeanFactoryTransactionAttributeSourceAdvisor作为PointcutAdvisor,既要提供Pointcut信息,又要提供Advice信息,
所以依赖AnnotationTransactionAttributeSource和TransactionInterceptor
```

```text
需要在bean实例化被使用前生成代理对象
AnnotationAwareAspectJAutoProxyCreator作为BeanPostProcessor,可以完成.

为了判断一个bean是否需要代理,需要依赖AnnotationTransactionAttributeSource
为了给代理对象增加切面功能,需要TransactionInterceptor
```

## 事务的自动装配

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