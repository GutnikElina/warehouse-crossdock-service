package com.innowise.warehousecrossdock.exception;

import static com.innowise.warehousecrossdock.constant.ExceptionMessage.GATE_INCOMPATIBLE_EXCEPTION_MESSAGE;

public class IncompatibleGateException extends RuntimeException {
    public IncompatibleGateException() {
        super(GATE_INCOMPATIBLE_EXCEPTION_MESSAGE);
    }
}
