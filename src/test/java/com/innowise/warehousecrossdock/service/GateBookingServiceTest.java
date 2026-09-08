package com.innowise.warehousecrossdock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.innowise.warehousecrossdock.dto.SearchAvailableSlotsRequest;
import com.innowise.warehousecrossdock.model.DockGate;
import com.innowise.warehousecrossdock.model.GateBookingSlot;
import com.innowise.warehousecrossdock.model.TemperatureMode;
import com.innowise.warehousecrossdock.model.TransportType;
import com.innowise.warehousecrossdock.model.WarehouseHub;
import com.innowise.warehousecrossdock.repository.GateBookingSlotRepository;
import com.innowise.warehousecrossdock.repository.GateRepository;
import com.innowise.warehousecrossdock.repository.HubRepository;
import com.innowise.warehousecrossdock.service.impl.GateBookingServiceImpl;
import io.hypersistence.utils.hibernate.type.range.Range;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GateBookingServiceTest {

    @Mock
    private HubRepository hubRepository;
    @Mock
    private GateRepository gateRepository;
    @Mock
    private GateBookingSlotRepository slotRepository;

    @InjectMocks
    private GateBookingServiceImpl service;

    private UUID hubId;
    private WarehouseHub hub;
    private DockGate gate;

    @BeforeEach
    void setUp() {
        hubId = UUID.randomUUID();
        hub = new WarehouseHub(
                hubId, "Main Hub", "Address", "Europe/Warsaw",
                LocalTime.of(8, 0), LocalTime.of(20, 0));
        gate = new DockGate(
                UUID.randomUUID(), hubId, "Gate 1",
                TemperatureMode.DRY, TransportType.TRUCK);
    }

    @Test
    void searchAvailableSlots_ReturnsValidIntervals_WhenNoBookings() {
        var request = new SearchAvailableSlotsRequest(
                OffsetDateTime.parse("2026-09-08T09:00:00+02:00"),
                OffsetDateTime.parse("2026-09-08T12:00:00+02:00"),
                TransportType.TRUCK,
                TemperatureMode.DRY,
                Duration.ofHours(1));

        when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
        when(gateRepository.findByHubIdAndCriteria(hubId, TransportType.TRUCK))
            .thenReturn(List.of(gate));
        when(slotRepository.findOverlappingForGates(any(), any(), any())).thenReturn(List.of());

        var result = service.searchAvailableSlots(hubId, request);

        assertEquals(1, result.size());
        assertEquals(gate.getId(), result.get(0).gateId());

        var interval = result.get(0).availableIntervals().get(0);
        assertEquals(request.searchFrom(), interval.startTime());
        assertEquals(request.searchTo(), interval.endTime());
    }

    @Test
    void searchAvailableSlots_ExcludesBookedIntervals() {
        var request = new SearchAvailableSlotsRequest(
                OffsetDateTime.parse("2026-09-08T09:00:00+02:00"),
                OffsetDateTime.parse("2026-09-08T12:00:00+02:00"),
                TransportType.TRUCK, TemperatureMode.DRY, Duration.ofHours(1));

        var bookedSlot = mock(GateBookingSlot.class);
        var range = Range.closedOpen(
                ZonedDateTime.parse("2026-09-08T10:00:00+02:00"),
                ZonedDateTime.parse("2026-09-08T11:00:00+02:00"));
        when(bookedSlot.getGateId()).thenReturn(gate.getId());
        when(bookedSlot.getBookingInterval()).thenReturn(range);

        when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
        when(gateRepository.findByHubIdAndCriteria(hubId, TransportType.TRUCK))
            .thenReturn(List.of(gate));
        when(slotRepository.findOverlappingForGates(any(), any(), any()))
            .thenReturn(List.of(bookedSlot));

        var result = service.searchAvailableSlots(hubId, request);

        var intervals = result.get(0).availableIntervals();
        assertEquals(2, intervals.size());
        assertEquals(OffsetDateTime.parse("2026-09-08T10:00:00+02:00"), intervals.get(0).endTime());
        assertEquals(OffsetDateTime.parse("2026-09-08T11:00:00+02:00"),
                intervals.get(1).startTime());
    }
}