package com.innowise.warehousecrossdock.service;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.facade.GateLockFacade;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GateBookingService {

  private static final String RESERVATION_TIMER_NAME = "dock_slot_reservation_duration_seconds";

  private final GateLockFacade gateLockFacade;
  private final GateBookingTransactionalOps transactionalOps;
  private final MeterRegistry meterRegistry;

  public ReserveSlotResponse reserveSlot(UUID hubId, ReserveSlotRequest request) {
    Timer.Sample sample = Timer.start(meterRegistry);
    try {
      return gateLockFacade.executeWithGateLock(
              request.gateId(),
              () -> transactionalOps.checkAndBook(hubId, request));
    } finally {
      sample.stop(meterRegistry.timer(RESERVATION_TIMER_NAME));
    }
  }
}

