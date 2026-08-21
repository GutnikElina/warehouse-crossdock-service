package com.innowise.warehousecrossdock.exception;

import static com.innowise.warehousecrossdock.constant.ExceptionMessage.*;

public class GateSlotAlreadyLockedException extends RuntimeException {
    public GateSlotAlreadyLockedException(String lockKey) {
        super(GATE_SLOT_ALREADY_LOCKED_EXCEPTION_MESSAGE.formatted(lockKey));
    }
}
