package com.innowise.warehousecrossdock.exception;

import com.innowise.warehousecrossdock.constant.ExceptionMessage;

public class HubNotFoundException extends RuntimeException {
    public HubNotFoundException() {
        super(ExceptionMessage.HUB_NOT_FOUND_EXCEPTION_MESSAGE);
    }
}
