package com.redis.demo;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

class DemoApplicationTests extends BaseTest{

	@Resource
	private RedisTemplate<String, Object> redisTemplate;

	@Test
	void test1() {
		redisTemplate.opsForValue().set("wahaha", 188);
		Object value = redisTemplate.opsForValue().get("wahaha");
		System.out.println("value = " + value);
	}

}
