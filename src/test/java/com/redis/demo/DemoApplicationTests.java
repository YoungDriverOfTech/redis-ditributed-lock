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
		myDistributionLock.lock(key, value, (100L * 1000L));
	}

	@Test
	void test3() {
		String key = "lockKey";
		String value = UUID.randomUUID().toString();
		boolean result = myDistributionLock.tryLock(key, value, (100L * 1000L));
		System.out.println("first time lock result = " + result);

		boolean result2 = myDistributionLock.tryLock("wahaha", value, (100L * 1000L));
		System.out.println("second time lock result = " + result2);

	}

	@Test
	void test4() {
		String key = "tryLock2";
		String value = UUID.randomUUID().toString();
		myDistributionLock.lock(key, value, 10 * 1000);

		// Get lock and wait time is 5s
		boolean firstLock = myDistributionLock.tryLock(key, value, 10 * 1000, 5 * 1000);
		System.out.println("firstLock = " + firstLock);

		// Get lock and wait time is 10s
		boolean secondLock = myDistributionLock.tryLock(key, value, 10 * 1000, 10 * 1000);
		System.out.println("secondLock = " + secondLock);
	}

	@Test
	void test5() {
		String key = "unlock";
		String value = UUID.randomUUID().toString();

		// lock
		myDistributionLock.lock(key, value, 30 * 1000);

		// first unlock with different value > NG
		myDistributionLock.unlock(key, UUID.randomUUID().toString());

		// second unlock with same value > OK
		myDistributionLock.unlock(key, value);
	}
}
