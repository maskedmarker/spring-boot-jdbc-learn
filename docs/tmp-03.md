# spring-boot-jdbc


### DataSourceAutoConfiguration
DataSourceAutoConfiguration更多的是一个auto-configure的入口点,DataSource-bean不是由DataSourceAutoConfiguration完成的
DataSourceAutoConfiguration做了如下工作:
1.引入了spring.datasource配置项
2.为容器提供DataSourcePool元信息
3.引入了DataSourceInitializationConfiguration,其会注册bean,用来执行DataSource初始化脚本
4.引入了PooledDataSourceConfiguration
5.当存在嵌入式数据库时,引入了EmbeddedDatabaseConfiguration(相应的EmbeddedDataSource由EmbeddedDataSourceConfiguration完成)

```text
@EnableConfigurationProperties(DataSourceProperties.class)
@Import({ DataSourcePoolMetadataProvidersConfiguration.class, DataSourceInitializationConfiguration.class })
public class DataSourceAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@Conditional(EmbeddedDatabaseCondition.class)
	@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
	@Import(EmbeddedDataSourceConfiguration.class)
	protected static class EmbeddedDatabaseConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(PooledDataSourceCondition.class)
	@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
	@Import({ DataSourceConfiguration.Hikari.class, DataSourceConfiguration.Tomcat.class,
			DataSourceConfiguration.Dbcp2.class, DataSourceConfiguration.Generic.class,
			DataSourceJmxConfiguration.class })
	protected static class PooledDataSourceConfiguration {
	}
}
```

### PooledDataSourceConfiguration

主要目的是为容器提供datasource连接池

```text
	@Configuration(proxyBeanMethods = false)
	@Conditional(PooledDataSourceCondition.class)
	@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
	@Import({ DataSourceConfiguration.Hikari.class, DataSourceConfiguration.Tomcat.class,
			DataSourceConfiguration.Dbcp2.class, DataSourceConfiguration.Generic.class,
			DataSourceJmxConfiguration.class })
	protected static class PooledDataSourceConfiguration {
	}
```

### DataSourceInitializationConfiguration

最终执行datasource初始化脚本的是DataSourceInitializerInvoker

```text
@Configuration(proxyBeanMethods = false)
@Import({ DataSourceInitializerInvoker.class, DataSourceInitializationConfiguration.Registrar.class })
class DataSourceInitializationConfiguration {

	static class Registrar implements ImportBeanDefinitionRegistrar {

		private static final String BEAN_NAME = "dataSourceInitializerPostProcessor";

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (!registry.containsBeanDefinition(BEAN_NAME)) {
				GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
				beanDefinition.setBeanClass(DataSourceInitializerPostProcessor.class);
				beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				// We don't need this one to be post processed otherwise it can cause a cascade of bean instantiation that we would rather avoid.
				beanDefinition.setSynthetic(true);
				registry.registerBeanDefinition(BEAN_NAME, beanDefinition);
			}
		}
	}
}
```


DataSourceInitializerInvoker

默认执行classpath下的schema-*.sql和data-*.sql.
可以在配置文件中指定相应的文件名

```text
Bean to handle DataSource initialization by running schema-*.sql on InitializingBean.afterPropertiesSet() and data-*.sql SQL scripts on a DataSourceSchemaCreatedEvent.
```