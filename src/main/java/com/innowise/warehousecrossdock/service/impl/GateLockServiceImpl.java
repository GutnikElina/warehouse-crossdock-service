package com.innowise.warehousecrossdock.service.impl;

import com.innowise.warehousecrossdock.service.GateLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class GateLockServiceImpl implements GateLockService {

    private static final long DEFAULT_WAIT_TIME = 1L;
    private static final long DEFAULT_LEASE_TIME = 5L;
    private static final String GATE_LOCK_PATTERN = "lock:gate:%s";

    private final DistributedLockExecutorServiceImpl lockExecutor;

    public <T> T executeWithGateLock(UUID gateId, Supplier<T> task) {
        String lockKey = GATE_LOCK_PATTERN.formatted(gateId);
        return lockExecutor.executeWithLock(lockKey, DEFAULT_WAIT_TIME,
                DEFAULT_LEASE_TIME, TimeUnit.SECONDS, task);
    }
}
