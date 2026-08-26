package com.innowise.warehousecrossdock.constant;

public final class ExceptionMessage {

    private ExceptionMessage() {}

    public static final String GATE_SLOT_ALREADY_LOCKED_EXCEPTION_MESSAGE =
            "The requested gate is currently locked by another process. Please try again.";

    public static final String GATE_BOOKING_INTERRUPTED_EXCEPTION_MESSAGE =
            "The booking operation was interrupted. Please try again.";

    public static final String DATA_INTEGRITY_VIOLATION_EXCEPTION =
            "The selected time interval overlaps with the existing booking.";

    public static final String GATE_NOT_FOUND_EXCEPTION_MESSAGE =
            "The requested gate was not found for the specified hub.";

    public static final String GATE_INCOMPATIBLE_EXCEPTION_MESSAGE =
            "The gate does not support the requested transport type or temperature mode.";

}
