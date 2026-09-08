package com.innowise.warehousecrossdock.exception;

import com.innowise.warehousecrossdock.constant.ExceptionMessage;

public class HubClosedException extends RuntimeException {
    public HubClosedException() {
        super(ExceptionMessage.HUB_CLOSED_EXCEPTION_MESSAGE);
    }
}
