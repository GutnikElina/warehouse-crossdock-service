package com.innowise.warehousecrossdock.service.impl;

import com.innowise.warehousecrossdock.exception.GateBookingInterruptedException;
import com.innowise.warehousecrossdock.exception.GateSlotAlreadyLockedException;
import com.innowise.warehousecrossdock.service.DistributedLockExecutorService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class DistributedLockExecutorServiceImpl implements DistributedLockExecutorService {

    private final RedissonClient redissonClient;

    @SuppressWarnings("java:S2222")
    public <T> T executeWithLock(String lockKey, long waitTime,
            long leaseTime, TimeUnit unit,
            Supplier<T> task) {
        var lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(waitTime, leaseTime, unit)) {
                throw new GateSlotAlreadyLockedException();
            }
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GateBookingInterruptedException();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
