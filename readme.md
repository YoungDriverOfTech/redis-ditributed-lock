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

# Distribution lock
## lock implement
```java
package com.redis.demo.lock;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Service
public class MyDistributionLock {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public void lock(String key, String value, long expireTime) {
        // self rotate
        while (true) {
            System.out.println("Normal Lock: Trying to get lock");
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.MILLISECONDS))) {
                System.out.println("Normal Lock: Got lock success");
                return;
            } else {
                System.out.println("Normal Lock: Got lock failed");
            }

            // blocked for a while
            LockSupport.parkNanos(this, TimeUnit.MICROSECONDS.toNanos(1000));
        }
    }

    public boolean tryLock(String key, String value, long expireTime) {
        // self rotate
        System.out.println("TryLock: Trying to get lock");
        if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.MILLISECONDS))) {
            System.out.println("TryLock: Got lock success");
            return true;
        } else {
            System.out.println("TryLock: Got lock failed");
            return false;
        }
    }

    public boolean tryLock(String key, String value, long expireTime, long waitTime) {
        long start = System.currentTimeMillis();

        while (true) {
            // self rotate
            System.out.println("TryLock: Trying to get lock");
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.MILLISECONDS))) {
                System.out.println("TryLock: Got lock success");
                return true;
            } else {
                System.out.println("TryLock: Got lock failed, wait next time to get lock");
            }

            // check wait time is over or not
            if (System.currentTimeMillis() - start > waitTime) {
                System.out.println("wait time is too long, get lock failed");
                return false;
            }
            // blocked for a while
            LockSupport.parkNanos(this, TimeUnit.MICROSECONDS.toNanos(1000));
        }
    }

    // Compare value to prevent wrong lock
    // Using lua script to ensure unlock atomic (Regard compare value and delete value as one action)
    public void unlock(String key, String value) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(key), value);
        if (Objects.equals(1L, result)) {
            System.out.println("release lock OK");
        } else {
            System.out.println("release lock NG");
        }
    }

}
```

## How to use
```java
package com.redis.demo.service;

import com.redis.demo.lock.MyDistributionLock;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MyService {

    @Resource
    private MyDistributionLock myDistributionLock;

    /**
     * Get the lock no matter how many time spend
     *
     * @param orderId order
     */
    public void doBusiness(long orderId) {
        // get lock
        String value = UUID.randomUUID().toString();
        myDistributionLock.lock(String.valueOf(orderId), value, 10 * 1000);

        // execute business lock
        System.out.println("business lock");

        // unlock
        myDistributionLock.unlock(String.valueOf(orderId), value);
    }

    /**
     * Get the lock without wait time
     *
     * @param orderId orderId
     */
    public void doBusines2(long orderId) {
        // get lock
        String value = UUID.randomUUID().toString();

        if (myDistributionLock.tryLock(String.valueOf(orderId), value, 10 * 1000)) {
            // execute business lock after got the lock
            System.out.println("business lock");

            // unlock
            myDistributionLock.unlock(String.valueOf(orderId), value);
        } else {
            System.out.println("didn't got the lock, can not execute business lock");
        }
    }

    /**
     * Get the lock with wait time
     *
     * @param orderId orderId
     */
    public void doBusines3(long orderId) {
        // get lock
        String value = UUID.randomUUID().toString();

        if (myDistributionLock.tryLock(String.valueOf(orderId), value, 10 * 1000, 10 * 1000)) {
            // execute business lock after got the lock
            System.out.println("business lock");

            // unlock
            myDistributionLock.unlock(String.valueOf(orderId), value);
        } else {
            System.out.println("didn't got the lock, can not execute business lock");
        }
    }
}

```