package com.innowise.warehousecrossdock.service;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;

import java.util.UUID;

public interface GateBookingTransactionalOps {
    ReserveSlotResponse checkAndBook(UUID hubId, ReserveSlotRequest request);
}
