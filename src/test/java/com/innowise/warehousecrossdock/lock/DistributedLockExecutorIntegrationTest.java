package com.innowise.warehousecrossdock.lock;

import com.innowise.warehousecrossdock.exception.GateSlotAlreadyLockedException;
import com.innowise.warehousecrossdock.util.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DistributedLockExecutorIntegrationTest extends AbstractIntegrationTest {

    private RedissonClient redissonClient;
    private DistributedLockExecutor executor;

    public static final String FIRST_THREAD_EXPECTED_RESULT = "Thread A done";
    public static final String SECOND_THREAD_EXPECTED_RESULT = "Thread B done";

    @BeforeEach
    void setUp() {
        Config config = new Config();
        config
            .useSingleServer()
            .setAddress("redis://" + REDIS_CONTAINER.getHost() + ":"
                    + REDIS_CONTAINER.getMappedPort(6379));

        redissonClient = Redisson.create(config);
        executor = new DistributedLockExecutor(redissonClient);
    }

    @AfterEach
    void tearDown() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    @Test
    void shouldThrowExceptionWhenSecondThreadTriesToAcquireSameLock() throws Exception {
        final String lockKey = "gate-slot-101";
        final int counter = 1;

        var lockAcquiredByThread = new CountDownLatch(counter);
        var allowThreadToFinishLock = new CountDownLatch(counter);

        var firstThread = CompletableFuture.runAsync(
                () -> executor.executeWithLock(lockKey, 100,
                        2000, TimeUnit.MILLISECONDS, () -> {
                            lockAcquiredByThread.countDown();
                            try {
                                allowThreadToFinishLock.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return FIRST_THREAD_EXPECTED_RESULT;
                        }));
        lockAcquiredByThread.await();

        var secondThread = CompletableFuture.runAsync(
                () -> executor.executeWithLock(lockKey, 100,
                        2000, TimeUnit.MILLISECONDS, () -> SECOND_THREAD_EXPECTED_RESULT));

        var exception = assertThrows(ExecutionException.class, secondThread::get);
        assertThat(exception.getCause()).isInstanceOf(GateSlotAlreadyLockedException.class);

        allowThreadToFinishLock.countDown();
        firstThread.get();
    }

    @Test
    void shouldAllowSecondThreadToAcquireLockAfterFirstReleasesIt() {
        final String lockKey = "gate-slot-102";

        var firstThreadResult = executor.executeWithLock(lockKey, 100,
                1000, TimeUnit.MILLISECONDS, () -> FIRST_THREAD_EXPECTED_RESULT);
        var secondThreadResult = executor.executeWithLock(lockKey, 100,
                1000, TimeUnit.MILLISECONDS, () -> SECOND_THREAD_EXPECTED_RESULT);

        assertThat(firstThreadResult).isEqualTo(FIRST_THREAD_EXPECTED_RESULT);
        assertThat(secondThreadResult).isEqualTo(SECOND_THREAD_EXPECTED_RESULT);
    }
}
