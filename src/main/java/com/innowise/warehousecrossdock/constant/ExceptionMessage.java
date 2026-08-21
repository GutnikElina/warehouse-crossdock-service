package com.innowise.warehousecrossdock.constant;

public final class ExceptionMessage {

    private ExceptionMessage() {}

    public static final String GATE_SLOT_ALREADY_LOCKED_EXCEPTION_MESSAGE =
            "Gate slot already locked by key [%s]";

    public static final String GATE_BOOKING_INTERRUPTED_EXCEPTION_MESSAGE =
            "Thread was interrupted while wait [%s]";
}
