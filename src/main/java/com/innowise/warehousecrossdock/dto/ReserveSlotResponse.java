package com.innowise.warehousecrossdock.dto;

import com.innowise.warehousecrossdock.model.GateBookingSlot;
import com.innowise.warehousecrossdock.model.GateBookingStatus;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReserveSlotResponse(
        UUID slotId,
        UUID gateId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        GateBookingStatus status
) {
  public static ReserveSlotResponse from(GateBookingSlot slot) {
    return new ReserveSlotResponse(slot.getId(), slot.getGateId(),
            slot.getBookingInterval().lower().toOffsetDateTime(),
            slot.getBookingInterval().upper().toOffsetDateTime().minus(Duration.ofMinutes(15)),
            slot.getStatus());
  }
}

