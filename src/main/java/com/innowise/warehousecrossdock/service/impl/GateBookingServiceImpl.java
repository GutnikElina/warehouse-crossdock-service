package com.innowise.warehousecrossdock.service.impl;

import com.innowise.warehousecrossdock.dto.*;
import com.innowise.warehousecrossdock.exception.GateSlotAlreadyLockedException;
import com.innowise.warehousecrossdock.exception.HubNotFoundException;
import com.innowise.warehousecrossdock.exception.NoAvailableGatesException;
import com.innowise.warehousecrossdock.exception.SlotAlreadyBookedException;
import com.innowise.warehousecrossdock.facade.GateLockFacade;
import com.innowise.warehousecrossdock.model.DockGate;
import com.innowise.warehousecrossdock.model.GateBookingSlot;
import com.innowise.warehousecrossdock.model.WarehouseHub;
import com.innowise.warehousecrossdock.repository.GateBookingSlotRepository;
import com.innowise.warehousecrossdock.repository.GateRepository;
import com.innowise.warehousecrossdock.repository.HubRepository;
import com.innowise.warehousecrossdock.service.GateBookingService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GateBookingServiceImpl implements GateBookingService {

    private static final String RESERVATION_TIMER_NAME = "dock_slot_reservation_duration_seconds";

    private final GateLockFacade gateLockFacade;
    private final GateBookingTransactionalOps transactionalOps;
    private final HubRepository hubRepository;
    private final GateRepository gateRepository;
    private final GateBookingSlotRepository slotRepository;
    private final MeterRegistry meterRegistry;

    @Override
    public ReserveSlotResponse reserveSlot(UUID hubId, ReserveSlotRequest request) {
        var sample = Timer.start(meterRegistry);
        try {
            if (request.gateId() != null) {
                return gateLockFacade.executeWithGateLock(
                        request.gateId(),
                        () -> transactionalOps.checkAndBook(hubId, request));
            }

            return autoBookFirstAvailable(hubId, request);

        } finally {
            sample.stop(meterRegistry.timer(RESERVATION_TIMER_NAME));
        }
    }

    public List<AvailableGateSlotsResponse> searchAvailableSlots(UUID hubId,
            SearchAvailableSlotsRequest request) {
        var hub = hubRepository.findById(hubId).orElseThrow(HubNotFoundException::new);

        var eligibleGates = gateRepository.findByHubIdAndCriteria(hubId, request.transportType())
            .stream()
            .filter(gate -> gate.matchesTemperature(request.requiredTemperatureMode()))
            .toList();

        if (eligibleGates.isEmpty()) {
            return List.of();
        }

        var gateIds = eligibleGates.stream().map(DockGate::getId).toList();

        var existingSlots = slotRepository.findOverlappingForGates(
                gateIds,
                request.searchFrom().toZonedDateTime(),
                request.searchTo().toZonedDateTime());

        var slotsByGate = existingSlots.stream()
            .collect(Collectors.groupingBy(GateBookingSlot::getGateId));

        return eligibleGates.stream()
            .map(gate -> {
                var bookedIntervals = slotsByGate.getOrDefault(gate.getId(), List.of());
                var freeIntervals = calculateFreeIntervals(
                        hub, request.searchFrom(), request.searchTo(), request.requiredDuration(),
                        bookedIntervals);
                return new AvailableGateSlotsResponse(gate.getId(), gate.getName(), freeIntervals);
            })
            .filter(response -> !response.availableIntervals().isEmpty())
            .toList();
    }

    private ReserveSlotResponse autoBookFirstAvailable(UUID hubId, ReserveSlotRequest request) {
        var duration = Duration.between(request.startTime(), request.endTime());
        var searchReq = new SearchAvailableSlotsRequest(
                request.startTime(), request.endTime(), request.transportType(),
                request.temperatureMode(), duration);

        var availableGates = searchAvailableSlots(hubId, searchReq);

        for (var availableGate : availableGates) {
            try {
                var requestWithGate = request.withGateId(availableGate.gateId());
                return gateLockFacade.executeWithGateLock(
                        availableGate.gateId(),
                        () -> transactionalOps.checkAndBook(hubId, requestWithGate));
            } catch (GateSlotAlreadyLockedException | SlotAlreadyBookedException e) {
            }
        }
        throw new NoAvailableGatesException();
    }

    private List<TimeInterval> calculateFreeIntervals(WarehouseHub hub, OffsetDateTime searchFrom,
            OffsetDateTime searchTo, Duration minDuration,
            List<GateBookingSlot> bookedSlots) {
        var freeIntervals = new ArrayList<TimeInterval>();
        var currentPointer = searchFrom;

        for (var booked : bookedSlots) {
            var bookedStart = booked.getBookingInterval().lower().toOffsetDateTime();
            var bookedEnd = booked.getBookingInterval().upper().toOffsetDateTime();

            if (currentPointer.isBefore(bookedStart)) {
                addIfValidLengthAndWorkingHours(hub, currentPointer, bookedStart, minDuration,
                        freeIntervals);
            }
            if (currentPointer.isBefore(bookedEnd)) {
                currentPointer = bookedEnd;
            }
        }

        if (currentPointer.isBefore(searchTo)) {
            addIfValidLengthAndWorkingHours(hub, currentPointer, searchTo, minDuration,
                    freeIntervals);
        }

        return freeIntervals;
    }

    private void addIfValidLengthAndWorkingHours(WarehouseHub hub, OffsetDateTime start,
            OffsetDateTime end,
            Duration minDuration, List<TimeInterval> resultList) {
        var hubZone = hub.getZoneId();
        var hubWorkingStart = start.atZoneSameInstant(hubZone).with(hub.getWorkingHoursStart())
            .toOffsetDateTime();
        var hubWorkingEnd = start.atZoneSameInstant(hubZone).with(hub.getWorkingHoursEnd())
            .toOffsetDateTime();

        var actualStart = start.isAfter(hubWorkingStart) ? start : hubWorkingStart;
        var actualEnd = end.isBefore(hubWorkingEnd) ? end : hubWorkingEnd;

        if (actualStart.isBefore(actualEnd)
                && Duration.between(actualStart, actualEnd).compareTo(minDuration) >= 0) {
            resultList.add(new TimeInterval(actualStart, actualEnd));
        }
    }
}
