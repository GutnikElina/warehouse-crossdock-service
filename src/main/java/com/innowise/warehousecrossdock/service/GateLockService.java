package com.innowise.warehousecrossdock.service;

import java.util.UUID;
import java.util.function.Supplier;

public interface GateLockService {
    <T> T executeWithGateLock(UUID gateId, Supplier<T> task);
}
