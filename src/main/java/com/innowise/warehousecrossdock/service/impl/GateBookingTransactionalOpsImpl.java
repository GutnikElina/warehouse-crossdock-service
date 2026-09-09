package com.innowise.warehousecrossdock.service.impl;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.exception.GateNotFoundException;
import com.innowise.warehousecrossdock.exception.IncompatibleGateException;
import com.innowise.warehousecrossdock.exception.SlotAlreadyBookedException;
import com.innowise.warehousecrossdock.model.DockGate;
import com.innowise.warehousecrossdock.model.GateBookingSlot;
import com.innowise.warehousecrossdock.repository.GateBookingSlotRepository;
import com.innowise.warehousecrossdock.repository.GateRepository;
import com.innowise.warehousecrossdock.service.GateBookingTransactionalOps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GateBookingTransactionalOpsImpl implements GateBookingTransactionalOps {

    private final GateRepository gateRepository;
    private final GateBookingSlotRepository slotRepository;

    @Transactional
    public ReserveSlotResponse checkAndBook(UUID hubId, ReserveSlotRequest reserveSlotRequest) {
        var dockGate = gateRepository.findByIdAndHubId(reserveSlotRequest.gateId(), hubId)
            .orElseThrow(GateNotFoundException::new);
        if (!validateCompatibility(dockGate, reserveSlotRequest)) {
            throw new IncompatibleGateException();
        }
        if (isOverlapped(reserveSlotRequest)) {
            throw new SlotAlreadyBookedException();
        }
        var gateBookingSlot = GateBookingSlot.book(reserveSlotRequest);
        slotRepository.saveAndFlush(gateBookingSlot);
        return ReserveSlotResponse.from(gateBookingSlot);
    }

    private boolean isOverlapped(ReserveSlotRequest reserveSlotRequest) {
        return slotRepository.existsOverlapping(reserveSlotRequest.gateId(),
                reserveSlotRequest.startTime().toZonedDateTime(),
                reserveSlotRequest.endTime().toZonedDateTime());
    }

    private boolean validateCompatibility(DockGate gate, ReserveSlotRequest request) {
        return gate.supports(request.transportType()) &&
                gate.matchesTemperature(request.requiredTemperatureMode());
    }
}
