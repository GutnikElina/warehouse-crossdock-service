package com.innowise.warehousecrossdock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.exception.GateSlotAlreadyLockedException;
import com.innowise.warehousecrossdock.facade.GateLockFacade;
import com.innowise.warehousecrossdock.model.TemperatureMode;
import com.innowise.warehousecrossdock.model.TransportType;
import com.innowise.warehousecrossdock.repository.GateBookingSlotRepository;
import com.innowise.warehousecrossdock.repository.GateRepository;
import com.innowise.warehousecrossdock.repository.HubRepository;
import com.innowise.warehousecrossdock.service.impl.GateBookingServiceImpl;
import com.innowise.warehousecrossdock.service.impl.GateBookingTransactionalOps;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GateBookingServiceImplTest {

    @Mock
    private GateLockFacade gateLockFacade;
    @Mock
    private GateBookingTransactionalOps transactionalOps;
    @Mock
    private HubRepository hubRepository;
    @Mock
    private GateRepository gateRepository;
    @Mock
    private GateBookingSlotRepository slotRepository;

    private MeterRegistry meterRegistry;
    private GateBookingServiceImpl service;

    private final UUID hubId = UUID.randomUUID();
    private ReserveSlotRequest request;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new GateBookingServiceImpl(
                gateLockFacade,
                transactionalOps,
                hubRepository,
                gateRepository,
                slotRepository,
                meterRegistry);

        request = new ReserveSlotRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now(),
                OffsetDateTime.now().plusMinutes(45),
                TransportType.TRUCK,
                TemperatureMode.DRY);
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
        assertThat(meterRegistry.find("dock_slot_reservation_duration_seconds").timer())
            .isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void propagatesLockExceptions_andStillRecordsTheTimer() {
        when(gateLockFacade.executeWithGateLock(eq(request.gateId()), any(Supplier.class)))
            .thenThrow(new GateSlotAlreadyLockedException());

        assertThatThrownBy(() -> service.reserveSlot(hubId, request))
            .isInstanceOf(GateSlotAlreadyLockedException.class);

        assertThat(meterRegistry.find("dock_slot_reservation_duration_seconds").timer())
            .isNotNull();
    }
}