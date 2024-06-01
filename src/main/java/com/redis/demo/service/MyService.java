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
