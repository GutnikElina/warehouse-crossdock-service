package com.innowise.warehousecrossdock.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.innowise.warehousecrossdock.exception.GateSlotAlreadyLockedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class DistributedLockExecutorIntegrationTest {

  @Container
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);

  private RedissonClient redissonClient;
  private DistributedLockExecutor executor;

  @BeforeEach
  void setUp() {
    Config config = new Config();
    config
        .useSingleServer()
        .setAddress("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));

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
    String lockKey = "gate-slot-101";

    CountDownLatch lockAcquiredByA = new CountDownLatch(1);
    CountDownLatch allowAToFinish = new CountDownLatch(1);

    CompletableFuture<Void> threadA =
        CompletableFuture.runAsync(
            () ->
                executor.executeWithLock(
                    lockKey,
                    100,
                    2000,
                    TimeUnit.MILLISECONDS,
                    () -> {
                      lockAcquiredByA.countDown();
                      try {
                        allowAToFinish.await();
                      } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                      }
                      return "Thread A done";
                    }));

    lockAcquiredByA.await();

    CompletableFuture<Void> threadB =
        CompletableFuture.runAsync(
            () ->
                executor.executeWithLock(
                    lockKey, 100, 2000, TimeUnit.MILLISECONDS, () -> "Thread B done"));

    ExecutionException exception = assertThrows(ExecutionException.class, threadB::get);
    assertThat(exception.getCause()).isInstanceOf(GateSlotAlreadyLockedException.class);

    allowAToFinish.countDown();
    threadA.get();
  }

  @Test
  void shouldAllowSecondThreadToAcquireLockAfterFirstReleasesIt() {
    String lockKey = "gate-slot-102";

    String resultA =
        executor.executeWithLock(lockKey, 100, 1000, TimeUnit.MILLISECONDS, () -> "Result A");
    String resultB =
        executor.executeWithLock(lockKey, 100, 1000, TimeUnit.MILLISECONDS, () -> "Result B");

    assertThat(resultA).isEqualTo("Result A");
    assertThat(resultB).isEqualTo("Result B");
  }
}
