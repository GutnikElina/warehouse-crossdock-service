package com.innowise.warehousecrossdock.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.exception.GateSlotAlreadyLockedException;
import com.innowise.warehousecrossdock.facade.GateLockFacade;
import com.innowise.warehousecrossdock.model.TemperatureMode;
import com.innowise.warehousecrossdock.model.TransportType;
import com.innowise.warehousecrossdock.service.impl.GateBookingServiceImpl;
import com.innowise.warehousecrossdock.service.impl.GateBookingTransactionalOps;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GateBookingServiceImplTest {

  @Mock GateLockFacade gateLockFacade;
  @Mock GateBookingTransactionalOps transactionalOps;

  @InjectMocks GateBookingServiceImpl service;

  MeterRegistry meterRegistry = mock(MeterRegistry.class);
  MeterRegistry.Config config = mock(MeterRegistry.Config.class);
  Clock clock = mock(Clock.class);

  private final UUID hubId = UUID.randomUUID();
  private ReserveSlotRequest request;

  @BeforeEach
  void setUp() {
    request =
        new ReserveSlotRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            OffsetDateTime.now(),
            OffsetDateTime.now().plusMinutes(45),
            TransportType.TRUCK,
            TemperatureMode.DRY);
    when(meterRegistry.timer(anyString())).thenReturn(mock(Timer.class));
    when(meterRegistry.config()).thenReturn(config);
    when(config.clock()).thenReturn(clock);
  }

  @Test
  @SuppressWarnings("unchecked")
  void delegatesToTransactionalOps_throughTheGateLock() {
    ReserveSlotResponse expected = mock(ReserveSlotResponse.class);
    when(transactionalOps.checkAndBook(hubId, request)).thenReturn(expected);
    when(gateLockFacade.executeWithGateLock(eq(request.gateId()), any(Supplier.class)))
        .thenAnswer(inv -> inv.getArgument(1, Supplier.class).get());

    ReserveSlotResponse actual = service.reserveSlot(hubId, request);

    assertThat(actual).isSameAs(expected);
    verify(transactionalOps).checkAndBook(hubId, request);
    verify(meterRegistry).timer("dock_slot_reservation_duration_seconds");
  }

  @Test
  @SuppressWarnings("unchecked")
  void propagatesLockExceptions_andStillRecordsTheTimer() {
    when(gateLockFacade.executeWithGateLock(eq(request.gateId()), any(Supplier.class)))
        .thenThrow(new GateSlotAlreadyLockedException());

    assertThatThrownBy(() -> service.reserveSlot(hubId, request))
        .isInstanceOf(GateSlotAlreadyLockedException.class);
    verify(meterRegistry).timer("dock_slot_reservation_duration_seconds");
  }
}
