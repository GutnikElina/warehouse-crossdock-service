package com.innowise.warehousecrossdock.exception;

import static com.innowise.warehousecrossdock.constant.ExceptionMessage.GATE_BOOKING_INTERRUPTED_EXCEPTION_MESSAGE;

public class GateBookingInterruptedException extends RuntimeException {
    public GateBookingInterruptedException() {
        super(GATE_BOOKING_INTERRUPTED_EXCEPTION_MESSAGE);
    }
}
