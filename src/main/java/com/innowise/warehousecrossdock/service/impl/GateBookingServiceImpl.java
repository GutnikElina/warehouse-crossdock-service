package com.innowise.warehousecrossdock.service.impl;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.service.GateBookingService;
import com.innowise.warehousecrossdock.service.GateBookingTransactionalOps;
import com.innowise.warehousecrossdock.service.GateLockService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GateBookingServiceImpl implements GateBookingService {

    private final GateLockService gateLockService;
    private final GateBookingTransactionalOps transactionalOps;
    private final MeterRegistry meterRegistry;

    private static final String RESERVATION_TIMER_NAME = "dock_slot_reservation_duration_seconds";

    @Override
    public ReserveSlotResponse reserveSlot(UUID hubId, ReserveSlotRequest request) {
        var sample = Timer.start(meterRegistry);
        try {
            return gateLockService.executeWithGateLock(request.gateId(),
                    () -> transactionalOps.checkAndBook(hubId, request));
        } finally {
            sample.stop(meterRegistry.timer(RESERVATION_TIMER_NAME));
        }
    }
}
