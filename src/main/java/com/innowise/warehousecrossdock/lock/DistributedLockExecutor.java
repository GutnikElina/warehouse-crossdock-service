package com.innowise.warehousecrossdock.lock;

import com.innowise.warehousecrossdock.exception.GateBookingInterruptedException;
import com.innowise.warehousecrossdock.exception.GateSlotAlreadyLockedException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DistributedLockExecutor {

  private final RedissonClient redissonClient;

  public <T> T executeWithLock(
      String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> task) {

    RLock lock = redissonClient.getLock(lockKey);

    boolean acquired;

    try {
      acquired = lock.tryLock(waitTime, leaseTime, unit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new GateBookingInterruptedException();
    }

      if (!acquired) {
          throw new GateSlotAlreadyLockedException();
      }

      try{
          return task.get();
      } finally {
          if (lock.isHeldByCurrentThread()) {
              lock.unlock();
          }
      }

  }
}
