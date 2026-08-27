package com.innowise.warehousecrossdock.service.impl;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.facade.GateLockFacade;
import com.innowise.warehousecrossdock.service.GateBookingService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GateBookingServiceImpl implements GateBookingService {

  private static final String RESERVATION_TIMER_NAME = "dock_slot_reservation_duration_seconds";

  private final GateLockFacade gateLockFacade;
  private final GateBookingTransactionalOps transactionalOps;
  private final MeterRegistry meterRegistry;

  @Override
  public ReserveSlotResponse reserveSlot(UUID hubId, ReserveSlotRequest request) {
    var sample = Timer.start(meterRegistry);
    try {
      return gateLockFacade.executeWithGateLock(
          request.gateId(), () -> transactionalOps.checkAndBook(hubId, request));
    } finally {
      sample.stop(meterRegistry.timer(RESERVATION_TIMER_NAME));
    }
  }
}
