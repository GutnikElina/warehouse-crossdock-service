package com.innowise.warehousecrossdock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.exception.GateNotFoundException;
import com.innowise.warehousecrossdock.exception.HubClosedException;
import com.innowise.warehousecrossdock.exception.HubNotFoundException;
import com.innowise.warehousecrossdock.exception.IncompatibleGateException;
import com.innowise.warehousecrossdock.exception.SlotAlreadyBookedException;
import com.innowise.warehousecrossdock.model.DockGate;
import com.innowise.warehousecrossdock.model.GateBookingSlot;
import com.innowise.warehousecrossdock.model.GateBookingStatus;
import com.innowise.warehousecrossdock.model.TemperatureMode;
import com.innowise.warehousecrossdock.model.TransportType;
import com.innowise.warehousecrossdock.model.WarehouseHub;
import com.innowise.warehousecrossdock.repository.GateBookingSlotRepository;
import com.innowise.warehousecrossdock.repository.GateRepository;
import com.innowise.warehousecrossdock.repository.HubRepository;
import com.innowise.warehousecrossdock.service.impl.GateBookingTransactionalOps;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class GateBookingTransactionalOpsTest {

    @Mock
    private GateRepository gateRepository;
    @Mock
    private GateBookingSlotRepository slotRepository;
    @Mock
    private HubRepository hubRepository;

    @InjectMocks
    private GateBookingTransactionalOps ops;

    private final UUID hubId = UUID.randomUUID();
    private final UUID gateId = UUID.randomUUID();
    private ReserveSlotRequest request;
    private DockGate compatibleGate;
    private WarehouseHub hub;

    @BeforeEach
    void setUp() {
        request = new ReserveSlotRequest(
                gateId,
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-09-01T14:00:00Z"),
                OffsetDateTime.parse("2026-09-01T14:45:00Z"),
                TransportType.TRUCK,
                TemperatureMode.DRY);

        compatibleGate = new DockGate(gateId, hubId, "Gate A1", TemperatureMode.DRY,
                TransportType.TRUCK);

        hub = new WarehouseHub(
                hubId,
                "Hub 1",
                "New York",
                "UTC",
                LocalTime.of(8, 0),
                LocalTime.of(20, 0));
    }

    @Test
    void booksSlot_whenGateExistsAndNoOverlapAndHubOpen() {
        when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
        when(gateRepository.findByIdAndHubId(gateId, hubId))
            .thenReturn(Optional.of(compatibleGate));
        when(slotRepository.existsOverlapping(
                gateId, request.startTime().toZonedDateTime(), request.endTime().toZonedDateTime()))
            .thenReturn(false);

        ReserveSlotResponse response = ops.checkAndBook(hubId, request);

        assertThat(response.status()).isEqualTo(GateBookingStatus.BOOKED);
        verify(slotRepository).saveAndFlush(any(GateBookingSlot.class));
    }

    @Test
    void throwsHubClosedException_whenBookingTimeOutsideHubWorkingHours() {
        WarehouseHub restrictedHub = new WarehouseHub(
                hubId, "Hub 1", "NYC", "UTC", LocalTime.of(8, 0), LocalTime.of(12, 0));

        when(hubRepository.findById(hubId)).thenReturn(Optional.of(restrictedHub));

        assertThatThrownBy(() -> ops.checkAndBook(hubId, request))
            .isInstanceOf(HubClosedException.class);

        verify(gateRepository, never()).findByIdAndHubId(any(), any());
    }

    @Test
    void throwsHubNotFoundException_whenHubDoesNotExist() {
        when(hubRepository.findById(hubId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ops.checkAndBook(hubId, request))
            .isInstanceOf(HubNotFoundException.class);
    }

    @Test
    void throwsIncompatibleGate_whenTransportTypeNotSupported() {
        when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
        DockGate containerOnlyGate = new DockGate(gateId, hubId, "Gate B2", TemperatureMode.DRY,
                TransportType.CONTAINER_TRUCK);
        when(gateRepository.findByIdAndHubId(gateId, hubId))
            .thenReturn(Optional.of(containerOnlyGate));

        assertThatThrownBy(() -> ops.checkAndBook(hubId, request))
            .isInstanceOf(IncompatibleGateException.class);
        verify(slotRepository, never()).existsOverlapping(any(), any(), any());
    }

    @Test
    void throwsIncompatibleGate_whenGateIsTooWarmForFrozenCargo() {
        when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
        ReserveSlotRequest frozenCargoRequest = new ReserveSlotRequest(
                gateId,
                request.routeId(),
                request.startTime(),
                request.endTime(),
                TransportType.TRUCK,
                TemperatureMode.FROZEN);
        when(gateRepository.findByIdAndHubId(gateId, hubId))
            .thenReturn(Optional.of(compatibleGate));

        assertThatThrownBy(() -> ops.checkAndBook(hubId, frozenCargoRequest))
            .isInstanceOf(IncompatibleGateException.class);
    }

    @Test
    void throwsSlotAlreadyBooked_whenOverlapDetectedByPreCheck() {
        when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
        when(gateRepository.findByIdAndHubId(gateId, hubId))
            .thenReturn(Optional.of(compatibleGate));
        when(slotRepository.existsOverlapping(
                gateId, request.startTime().toZonedDateTime(), request.endTime().toZonedDateTime()))
            .thenReturn(true);

        assertThatThrownBy(() -> ops.checkAndBook(hubId, request))
            .isInstanceOf(SlotAlreadyBookedException.class);
        verify(slotRepository, never()).saveAndFlush(any());
    }

    @Test
    void propagatesDataIntegrityViolation_whenExcludeConstraintFiresOnInsert() {
        when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
        when(gateRepository.findByIdAndHubId(gateId, hubId))
            .thenReturn(Optional.of(compatibleGate));
        when(slotRepository.existsOverlapping(any(), any(), any())).thenReturn(false);
        when(slotRepository.saveAndFlush(any(GateBookingSlot.class)))
            .thenThrow(new DataIntegrityViolationException("no_overlapping_slots"));

        assertThatThrownBy(() -> ops.checkAndBook(hubId, request))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void throwsGateNotFound_whenGateDoesNotBelongToHub() {
        when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
        when(gateRepository.findByIdAndHubId(gateId, hubId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ops.checkAndBook(hubId, request))
            .isInstanceOf(GateNotFoundException.class);
    }
}