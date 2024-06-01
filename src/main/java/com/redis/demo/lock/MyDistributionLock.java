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
