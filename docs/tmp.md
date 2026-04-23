

```text
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {
}


AnnotationAwareAspectJAutoProxyCreator
```


```text
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.aop", name = "auto", havingValue = "true", matchIfMissing = true)
public class AopAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Advice.class)
	static class AspectJAutoProxyingConfiguration {

		@Configuration(proxyBeanMethods = false)
		@EnableAspectJAutoProxy(proxyTargetClass = false)
		@ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "false",
				matchIfMissing = false)
		static class JdkDynamicAutoProxyConfiguration {

		}

		@Configuration(proxyBeanMethods = false)
		@EnableAspectJAutoProxy(proxyTargetClass = true)
		@ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "true",
				matchIfMissing = true)
		static class CglibAutoProxyConfiguration {

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("org.aspectj.weaver.Advice")
	@ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "true",
			matchIfMissing = true)
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
InfrastructureAdvisorAutoProxyCreator是 SmartInstantiationAwareBeanPostProcessor

org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.postProcessAfterInitialization
    org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.wrapIfNecessary
        org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.getAdvicesAndAdvisorsForBean
        org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.createProxy
            ProxyFactory proxyFactory = new ProxyFactory();
            buildAdvisors
            customizeProxyFactory
            proxyFactory.getProxy
            
            

org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.buildAdvisors
            
```