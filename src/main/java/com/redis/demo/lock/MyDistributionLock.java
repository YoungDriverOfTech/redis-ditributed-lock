package com.redis.demo.lock;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Service
public class MyDistributionLock {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public void lock(String key, String value, long expireTime) {
        // self rotate
        while (true) {
            System.out.println("Trying to get lock");
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.SECONDS))) {
                System.out.println("Got lock success");
                return;
            } else {
                System.out.println("Got lock failed");
            }

            // blocked for a while
            LockSupport.parkNanos(this, TimeUnit.MICROSECONDS.toNanos(1000));
        }
    }
}
