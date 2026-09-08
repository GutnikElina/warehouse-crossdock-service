package com.innowise.warehousecrossdock.dto;

import java.time.OffsetDateTime;

public record TimeInterval(
        OffsetDateTime startTime,
        OffsetDateTime endTime) {
}
