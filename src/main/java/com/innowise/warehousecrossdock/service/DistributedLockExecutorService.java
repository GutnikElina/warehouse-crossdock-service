package com.innowise.warehousecrossdock.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface DistributedLockExecutorService {
    <T> T executeWithLock(String lockKey, long waitTime,
            long leaseTime, TimeUnit unit, Supplier<T> task);
}
