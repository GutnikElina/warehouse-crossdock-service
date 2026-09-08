package com.innowise.warehousecrossdock.exception;

import com.innowise.warehousecrossdock.constant.ExceptionMessage;

public class NoAvailableGatesException extends RuntimeException {
    public NoAvailableGatesException() {
        super(ExceptionMessage.NO_AVAILABLE_GATES_EXCEPTION_MESSAGE);
    }
}
