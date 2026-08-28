package com.innowise.warehousecrossdock.dto;

import com.innowise.warehousecrossdock.constant.ConfigValues;
import com.innowise.warehousecrossdock.model.GateBookingSlot;
import com.innowise.warehousecrossdock.model.GateBookingStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReserveSlotResponse(
        UUID slotId,
        UUID gateId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        GateBookingStatus status) {
    public static ReserveSlotResponse from(GateBookingSlot slot) {
        return new ReserveSlotResponse(
                slot.getId(),
                slot.getGateId(),
                slot.getBookingInterval().lower().toOffsetDateTime(),
                slot.getBookingInterval()
                    .upper()
                    .toOffsetDateTime()
                    .minus(ConfigValues.SLOT_BOOKING_INTERVAL),
                slot.getStatus());
    }
}
