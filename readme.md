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