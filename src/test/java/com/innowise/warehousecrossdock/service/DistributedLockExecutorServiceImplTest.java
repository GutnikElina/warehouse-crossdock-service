package com.innowise.warehousecrossdock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.innowise.warehousecrossdock.exception.GateBookingInterruptedException;
import com.innowise.warehousecrossdock.exception.GateSlotAlreadyLockedException;
import java.util.concurrent.TimeUnit;

import com.innowise.warehousecrossdock.service.impl.DistributedLockExecutorServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
class DistributedLockExecutorServiceImplTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private DistributedLockExecutorServiceImpl executor;

    @Test
    void shouldExecuteTaskAndReleaseLockWhenAcquired() throws Exception {
        when(redissonClient.getLock("gate-1")).thenReturn(lock);
        when(lock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        var result = executor.executeWithLock("gate-1", 5,
                10, TimeUnit.SECONDS, () -> "SUCCESS");

        assertEquals("SUCCESS", result);
        verify(lock).unlock();
    }

    @Test
    void shouldThrowGateSlotAlreadyLockedExceptionWhenLockFails() throws Exception {
        when(redissonClient.getLock("gate-1")).thenReturn(lock);
        when(lock.tryLock(1, 10, TimeUnit.SECONDS)).thenReturn(false);

        assertThrows(GateSlotAlreadyLockedException.class,
                () -> executor.executeWithLock("gate-1", 1,
                        10, TimeUnit.SECONDS, () -> "DATA"));
    }

    @Test
    void shouldRestoreInterruptStatusAndThrowCustomException() throws Exception {
        when(redissonClient.getLock("gate-1")).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenThrow(new InterruptedException());
        assertThrows(GateBookingInterruptedException.class,
                () -> executor.executeWithLock("gate-1", 1,
                        10, TimeUnit.SECONDS, () -> "DATA"));
        assertTrue(Thread.currentThread().isInterrupted());
    }
}
