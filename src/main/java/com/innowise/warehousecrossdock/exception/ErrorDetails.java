package com.innowise.warehousecrossdock.exception;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ErrorDetails {

  private final String message;

  private final String errorName;

  private final int httpStatus;

  private final Instant timestamp;
}
