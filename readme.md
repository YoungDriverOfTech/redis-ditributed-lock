# Setup
## Install

### Install redis
```shell
brew install redis
```

### Install redis client tool
Another redis desktop manager

### Add jedis dependency
https://mvnrepository.com/artifact/redis.clients/jedis/5.1.3
```xml
<!-- https://mvnrepository.com/artifact/redis.clients/jedis -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>5.1.3</version>
</dependency>
```

## Use jedis
```java
package com.redis.demo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {

		// Set redis pool config
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(8);
		poolConfig.setMaxTotal(18);

		// Initialize redis pool
		JedisPool jedisPool = new JedisPool(poolConfig, "127.0.0.1", 6379);

		// Get redis connection
		Jedis jedis = jedisPool.getResource();

		// Get value
		String value1 = jedis.get("key-1");
		System.out.println("value1 = " + value1);

		// Set value
		jedis.set("key-5", String.valueOf(113));
		String value2 = jedis.get("key-5");
		System.out.println("value1 = " + value2);

		// Return connection to pool
		jedis.close();

		// Close pool
		jedisPool.close();
	}
}
```

## Integrate Redis and Springboot
### import dependency
```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
			<exclusions>
                <!--   排除默认的访问redis类库，而使用redis-->
				<exclusion>
					<groupId>lettuce-core</groupId>
					<artifactId>io.lettuce</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
```

### 配置连接信息
```yaml
spring:
  application:
    name: redis-distributed-lock-demo
  redis:
    host: localhost
    port: 6379
    jedis:
      max-idle: 16
      max-active: 32
      min-idle: 8

server:
  port: 8080
  servlet:
    context-path: /distributedLock
```

### 配置类
```java
package com.redis.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}

```

### 编写测试方法
