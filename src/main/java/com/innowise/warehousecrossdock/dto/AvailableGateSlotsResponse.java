package com.innowise.warehousecrossdock.dto;

import java.util.UUID;
import java.util.List;

public record AvailableGateSlotsResponse(
        UUID gateId,
        String gateName,
        List<TimeInterval> availableIntervals) {
}
