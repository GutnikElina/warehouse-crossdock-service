package com.innowise.warehousecrossdock.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class ErrorDetails {

    private final String message;

    private final String errorName;

    private final int httpStatus;

    private final LocalDateTime timestamp;
}