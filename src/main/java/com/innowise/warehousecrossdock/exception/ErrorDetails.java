package com.innowise.warehousecrossdock.exception;

import java.time.Instant;

public record ErrorDetails(String message, Instant timestamp) {
}
