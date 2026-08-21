package com.innowise.warehousecrossdock.exception;
import static com.innowise.warehousecrossdock.constant.ExceptionMessage.*;

public class GateBookingInterruptedException extends RuntimeException {
    public GateBookingInterruptedException(String lockKey) {
        super(GATE_BOOKING_INTERRUPTED_EXCEPTION_MESSAGE.formatted(lockKey));
    }
}
