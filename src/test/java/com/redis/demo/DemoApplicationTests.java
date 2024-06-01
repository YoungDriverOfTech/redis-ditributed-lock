package com.redis.demo;

import com.redis.demo.lock.MyDistributionLock;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

class DemoApplicationTests extends BaseTest{

	@Resource
	private RedisTemplate<String, Object> redisTemplate;


	@Resource
	private MyDistributionLock myDistributionLock;

	@Test
	void test1() {
		redisTemplate.opsForValue().set("wahaha", 188);
		Object value = redisTemplate.opsForValue().get("wahaha");
		System.out.println("value = " + value);
	}

	@Test
	void test2() {
		String key = "lockKey";
		String value = UUID.randomUUID().toString();
		myDistributionLock.lock(key, value, 100 * 1000);
	}

}
