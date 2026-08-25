package com.innowise.warehousecrossdock.service;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.model.DockGate;
import com.innowise.warehousecrossdock.model.GateBookingSlot;
import com.innowise.warehousecrossdock.exception.*;
import com.innowise.warehousecrossdock.repository.GateBookingSlotRepository;
import com.innowise.warehousecrossdock.repository.GateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GateBookingTransactionalOps {

  private final GateRepository gateRepository;
  private final GateBookingSlotRepository slotRepository;

  private static final Duration BOOKING_BUFFER = Duration.ofMinutes(15);

  @Transactional
  public ReserveSlotResponse checkAndBook(UUID hubId, ReserveSlotRequest request) {

    DockGate gate = gateRepository.findByIdAndHubId(request.gateId(), hubId).orElseThrow(GateNotFoundException::new);

    validateCompatibility(gate, request);

    ZonedDateTime bufferedStart = request.startTime().toZonedDateTime().minus(BOOKING_BUFFER);
    ZonedDateTime bufferedEnd = request.endTime().toZonedDateTime().plus(BOOKING_BUFFER);


    boolean overlapExists = slotRepository.existsOverlapping(
            request.gateId(), bufferedStart, bufferedEnd);
    if (overlapExists) {
      throw new SlotAlreadyBookedException();
    }

    GateBookingSlot slot = GateBookingSlot.book(request);

    slotRepository.saveAndFlush(slot);

    return ReserveSlotResponse.from(slot);
  }

  private void validateCompatibility(DockGate gate, ReserveSlotRequest request) {
    if (!gate.supports(request.transportType())) {
      throw new IncompatibleGateException();
    }
    if (!gate.matchesTemperature(request.requiredTemperatureMode())) {
      throw new IncompatibleGateException();
    }
  }

}

