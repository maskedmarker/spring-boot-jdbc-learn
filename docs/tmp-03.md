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