package com.innowise.warehousecrossdock.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Builder
@Getter
@AllArgsConstructor
public class ErrorDetails {

    private final String message;

    private final String errorName;

    private final int httpStatus;

    private final Instant timestamp;
}
