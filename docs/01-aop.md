


```text
一、先一句话总结原理



Spring AOP 注解的本质就是：
    扫描 @Aspect 类，解析里面的 @Pointcut、@Before、@Around 等注解；
    把这些解析成切面、切点、通知；
    在 Bean 初始化后期，判断 Bean 是否匹配切点，匹配就生成代理对象（JDK/CGLIB）；
    调用方法时，代理拦截 → 执行通知逻辑 → 执行目标方法。
```

```text
二、核心流程（从启动到调用）



1. 开启 AOP 注解支持：@EnableAspectJAutoProxy
这个注解是入口，它做一件事：往容器里注册一个关键 Bean：AnnotationAwareAspectJAutoProxyCreator
它本质是一个：BeanPostProcessor（后置处理器） → 所有 Bean 初始化前后都会被它处理。

2. 扫描并解析 @Aspect 切面类
Spring 启动时：
找到所有标 @Aspect + @Component 的类；
解析类上的所有注解：
    @Pointcut → 解析切点表达式（AspectJ 表达式）
    @Before/@After/@Around → 解析成 Advice（通知器）
把所有切面统一封装成 Advisor 列表，存起来待用。
Advisor = PointCut（切点） + Advice（通知）

3. Bean 初始化时创建代理（核心步骤）
每个 Bean 初始化完成后，AnnotationAwareAspectJAutoProxyCreator 会做：
    遍历所有 Advisor；
    用切点表达式匹配当前 Bean 的类和方法；
    只要有任意一个方法匹配 → 需要创建代理；
    根据类是否有接口选择：
        有接口 → JDK 动态代理
        无接口 /proxyTargetClass=true → CGLIB 代理
    用代理对象替换原始 Bean，放入 Spring 容器。
以后你 @Autowired 注入的，都是代理对象，不是原始对象。

4. 方法调用时：代理拦截执行 AOP
当你调用代理对象的方法：
代理拦截方法调用；
找到匹配这个方法的所有 Advisor；
根据通知类型（Before/After/Around）构建拦截器链；
按顺序执行通知 + 目标方法：
    Around 前逻辑
    Before
    目标方法
    AfterReturning / AfterThrowing
    After
    Around 后逻辑
```

```text
org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#postProcessBeforeInstantiation

	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
		Object cacheKey = getCacheKey(beanClass, beanName);

		// ...

		TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
		if (targetSource != null) {
			// ...
			
			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);    // 从beanFactory中获取所有的Advisor实例,过滤出仅适用于当前bean的Advisor实例
			Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);                // 实例化一个proxy对象,该proxy对象包含上一步Advisor实例列表
			// ...
			return proxy;
		}

		return null;
	}
```


## AOP自动装配

spring-boot自动装配AOP的配置

```text
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.aop", name = "auto", havingValue = "true", matchIfMissing = true)
public class AopAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Advice.class)
	static class AspectJAutoProxyingConfiguration {

		@Configuration(proxyBeanMethods = false)
		@EnableAspectJAutoProxy(proxyTargetClass = false)
		@ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "false", matchIfMissing = false)
		static class JdkDynamicAutoProxyConfiguration {
		}

		@Configuration(proxyBeanMethods = false)
		@EnableAspectJAutoProxy(proxyTargetClass = true)
		@ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "true", matchIfMissing = true)
		static class CglibAutoProxyConfiguration {
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("org.aspectj.weaver.Advice")
	@ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "true", matchIfMissing = true)
	static class ClassProxyingConfiguration {

		ClassProxyingConfiguration(BeanFactory beanFactory) {
			if (beanFactory instanceof BeanDefinitionRegistry) {
				BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
				AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);                  // 向容器注册InfrastructureAdvisorAutoProxyCreator
				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);              // 强制将InfrastructureAdvisorAutoProxyCreator的属性proxyTargetClass设置为true
			}
		}

	}
}
```

```text
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {
}


AspectJAutoProxyRegistrar会注册AnnotationAwareAspectJAutoProxyCreator,引入这个BeanPostProcessor,此处是AOP框架的入口.
数据库事务的AOP只需要Advisor即可.
```