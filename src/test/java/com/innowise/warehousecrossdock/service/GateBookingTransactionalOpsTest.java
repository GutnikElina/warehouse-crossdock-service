package com.innowise.warehousecrossdock.service;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.model.*;
import com.innowise.warehousecrossdock.exception.GateNotFoundException;
import com.innowise.warehousecrossdock.exception.IncompatibleGateException;
import com.innowise.warehousecrossdock.exception.SlotAlreadyBookedException;
import com.innowise.warehousecrossdock.repository.GateBookingSlotRepository;
import com.innowise.warehousecrossdock.repository.GateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GateBookingTransactionalOpsTest {

  @Mock
  GateRepository gateRepository;
  @Mock
  GateBookingSlotRepository slotRepository;
  @InjectMocks
  GateBookingTransactionalOps ops;

  private final UUID hubId = UUID.randomUUID();
  private final UUID gateId = UUID.randomUUID();
  private ReserveSlotRequest request;
  private DockGate compatibleGate;

  private static final Duration BOOKING_BUFFER = Duration.ofMinutes(15);

  @BeforeEach
  void setUp() {
    request = new ReserveSlotRequest(gateId, UUID.randomUUID(),
            OffsetDateTime.parse("2026-09-01T14:00:00Z"),
            OffsetDateTime.parse("2026-09-01T14:45:00Z"),
            TransportType.TRUCK,
            TemperatureMode.DRY);
    compatibleGate = new DockGate(gateId, hubId, "Gate A1", TemperatureMode.DRY, TransportType.TRUCK);
  }

  @Test
  void booksSlot_whenGateExistsAndNoOverlap() {
    when(gateRepository.findByIdAndHubId(gateId, hubId))
            .thenReturn(Optional.of(compatibleGate));
    when(slotRepository.existsOverlapping(gateId,
            request.startTime().toZonedDateTime().minus(BOOKING_BUFFER),
            request.endTime().toZonedDateTime().plus(BOOKING_BUFFER)))
            .thenReturn(false);

    ReserveSlotResponse response = ops.checkAndBook(hubId, request);

    assertThat(response.status()).isEqualTo(GateBookingStatus.BOOKED);
    verify(slotRepository).saveAndFlush(any(GateBookingSlot.class));
  }

  @Test
  void throwsIncompatibleGate_whenTransportTypeNotSupported() {
    DockGate containerOnlyGate = new DockGate(gateId, hubId, "Gate B2",
            TemperatureMode.DRY, TransportType.CONTAINER_TRUCK);
    when(gateRepository.findByIdAndHubId(gateId, hubId))
            .thenReturn(Optional.of(containerOnlyGate));

    assertThatThrownBy(() -> ops.checkAndBook(hubId, request))
            .isInstanceOf(IncompatibleGateException.class);
    verify(slotRepository, never()).existsOverlapping(any(), any(), any());
  }

  @Test
  void throwsIncompatibleGate_whenGateIsTooWarmForFrozenCargo() {
    ReserveSlotRequest frozenCargoRequest = new ReserveSlotRequest(
            gateId, request.routeId(), request.startTime(), request.endTime(),
            TransportType.TRUCK, TemperatureMode.FROZEN);
    when(gateRepository.findByIdAndHubId(gateId, hubId))
            .thenReturn(Optional.of(compatibleGate));

    assertThatThrownBy(() -> ops.checkAndBook(hubId, frozenCargoRequest))
            .isInstanceOf(IncompatibleGateException.class);
  }



  @Test
  void throwsSlotAlreadyBooked_whenOverlapDetectedByPreCheck() {
    when(gateRepository.findByIdAndHubId(gateId, hubId))
            .thenReturn(Optional.of(compatibleGate));
    when(slotRepository.existsOverlapping(gateId,
            request.startTime().toZonedDateTime().minus(BOOKING_BUFFER),
            request.endTime().toZonedDateTime().plus(BOOKING_BUFFER)))
            .thenReturn(true);

    assertThatThrownBy(() -> ops.checkAndBook(hubId, request))
            .isInstanceOf(SlotAlreadyBookedException.class);
    verify(slotRepository, never()).saveAndFlush(any());
  }

  @Test
  void propagatesDataIntegrityViolation_whenExcludeConstraintFiresOnInsert() {
    when(gateRepository.findByIdAndHubId(gateId, hubId))
            .thenReturn(Optional.of(compatibleGate));
    when(slotRepository.existsOverlapping(any(), any(), any()))
            .thenReturn(false);
    when(slotRepository.saveAndFlush(any(GateBookingSlot.class)))
            .thenThrow(new DataIntegrityViolationException("no_overlapping_slots"));

    assertThatThrownBy(() -> ops.checkAndBook(hubId, request))
            .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void throwsGateNotFound_whenGateDoesNotBelongToHub() {
    when(gateRepository.findByIdAndHubId(gateId, hubId))
            .thenReturn(Optional.empty());

    assertThatThrownBy(() -> ops.checkAndBook(hubId, request))
            .isInstanceOf(GateNotFoundException.class);
  }
}
