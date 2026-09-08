package com.innowise.warehousecrossdock.service.impl;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.exception.*;
import com.innowise.warehousecrossdock.model.DockGate;
import com.innowise.warehousecrossdock.model.GateBookingSlot;
import com.innowise.warehousecrossdock.model.WarehouseHub;
import com.innowise.warehousecrossdock.repository.GateBookingSlotRepository;
import com.innowise.warehousecrossdock.repository.GateRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.innowise.warehousecrossdock.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GateBookingTransactionalOps {

    private final GateRepository gateRepository;
    private final GateBookingSlotRepository slotRepository;
    private final HubRepository hubRepository;

    @Transactional
    public ReserveSlotResponse checkAndBook(UUID hubId, ReserveSlotRequest request) {
        var hub = hubRepository.findById(hubId).orElseThrow(HubNotFoundException::new);
        validateWorkingHours(hub, request.startTime(), request.endTime());

        var gate = gateRepository.findByIdAndHubId(request.gateId(), hubId)
            .orElseThrow(GateNotFoundException::new);

        validateCompatibility(gate, request);

        boolean isOverlapping = slotRepository.existsOverlapping(
                request.gateId(),
                request.startTime().toZonedDateTime(),
                request.endTime().toZonedDateTime());

        if (isOverlapping) {
            throw new SlotAlreadyBookedException();
        }

        var slot = GateBookingSlot.book(request);
        slotRepository.saveAndFlush(slot);

        return ReserveSlotResponse.from(slot);
    }

    public void validateWorkingHours(WarehouseHub hub, OffsetDateTime start, OffsetDateTime end) {
        var hubZone = hub.getZoneId();
        var localStart = start.atZoneSameInstant(hubZone).toLocalTime();
        var localEnd = end.atZoneSameInstant(hubZone).toLocalTime();

        if (localStart.isBefore(hub.getWorkingHoursStart())
                || localEnd.isAfter(hub.getWorkingHoursEnd())) {
            throw new HubClosedException();
        }
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