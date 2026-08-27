package com.innowise.warehousecrossdock.service.impl;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.exception.*;
import com.innowise.warehousecrossdock.model.DockGate;
import com.innowise.warehousecrossdock.model.GateBookingSlot;
import com.innowise.warehousecrossdock.repository.GateBookingSlotRepository;
import com.innowise.warehousecrossdock.repository.GateRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GateBookingTransactionalOps {

  private final GateRepository gateRepository;
  private final GateBookingSlotRepository slotRepository;

  @Transactional
  public ReserveSlotResponse checkAndBook(UUID hubId, ReserveSlotRequest request) {

    DockGate gate =
        gateRepository
            .findByIdAndHubId(request.gateId(), hubId)
            .orElseThrow(GateNotFoundException::new);

    validateCompatibility(gate, request);

    Optional.of(
            slotRepository.existsOverlapping(
                request.gateId(),
                request.startTime().toZonedDateTime(),
                request.endTime().toZonedDateTime()))
        .filter(Boolean::booleanValue)
        .ifPresent(
            overlap -> {
              throw new SlotAlreadyBookedException();
            });

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
