package com.innowise.warehousecrossdock.service;

import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.exception.ErrorDetails;
import com.innowise.warehousecrossdock.model.GateBookingStatus;
import com.innowise.warehousecrossdock.model.TemperatureMode;
import com.innowise.warehousecrossdock.model.TransportType;
import com.innowise.warehousecrossdock.util.AbstractIntegrationTest;
import com.innowise.warehousecrossdock.util.GateTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;

class GateBookingReservationIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @Autowired
    private GateTestDataFactory gateTestDataFactory;

    private UUID hubId;
    private UUID gateId;
    private UUID routeId;

    public static final String SLOT_RESERVATION_URI = "/api/v1/hubs/{hubId}/slots/reserve";

    @BeforeEach
    void setUp() {
        this.restClient = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .build();
    }

    @Test
    void returns201_andPersistsSlot_onFirstReservation() {
        hubId = gateTestDataFactory.seedHub();
        gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();

        var reserveSlotRequest = gateTestDataFactory.requestFor(gateId, routeId,
                "2026-09-01T10:00:00Z", "2026-09-01T10:45:00Z",
                TransportType.TRUCK, TemperatureMode.DRY);

        var reserveSlotResponse = restClient.post()
            .uri(SLOT_RESERVATION_URI, hubId)
            .body(reserveSlotRequest)
            .retrieve()
            .toEntity(ReserveSlotResponse.class);

        assertThat(reserveSlotResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reserveSlotResponse.getBody()).isNotNull();
        assertThat(reserveSlotResponse.getBody().status()).isEqualTo(GateBookingStatus.BOOKED);
    }

    @Test
    void returns201_afterFifteenMinuteGap() {
        hubId = gateTestDataFactory.seedHub();
        gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();

        var firstReserveSlotRequest = gateTestDataFactory.requestFor(gateId, routeId,
                "2026-09-01T11:00:00Z", "2026-09-01T11:45:00Z",
                TransportType.TRUCK, TemperatureMode.DRY);
        var acceptedReserveSlotRequest = gateTestDataFactory.requestFor(gateId, routeId,
                "2026-09-01T12:00:00Z", "2026-09-01T12:45:00Z",
                TransportType.TRUCK, TemperatureMode.DRY);

        restClient.post()
            .uri(SLOT_RESERVATION_URI, hubId)
            .body(firstReserveSlotRequest)
            .retrieve()
            .toEntity(ReserveSlotResponse.class);
        var bookedSlotResponseEntity = restClient.post()
            .uri(SLOT_RESERVATION_URI, hubId)
            .body(acceptedReserveSlotRequest)
            .retrieve()
            .toEntity(ReserveSlotResponse.class);

        assertThat(bookedSlotResponseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bookedSlotResponseEntity.getBody()).isNotNull();
        assertThat(bookedSlotResponseEntity.getBody().status()).isEqualTo(GateBookingStatus.BOOKED);
    }

    @Test
    void returns409_onSecondOverlappingReservation() {
        hubId = gateTestDataFactory.seedHub();
        gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();

        var firstReserveSlotRequest = gateTestDataFactory.requestFor(gateId, routeId,
                "2026-09-01T11:00:00Z", "2026-09-01T11:45:00Z",
                TransportType.TRUCK, TemperatureMode.DRY);
        var overlappingReserveSlotRequest = gateTestDataFactory.requestFor(gateId, routeId,
                "2026-09-01T11:55:00Z", "2026-09-01T12:15:00Z",
                TransportType.TRUCK, TemperatureMode.DRY);

        restClient.post()
            .uri(SLOT_RESERVATION_URI, hubId)
            .body(firstReserveSlotRequest)
            .retrieve()
            .toEntity(ReserveSlotResponse.class);
        var exception = (HttpClientErrorException) catchException(
                () -> restClient.post()
                    .uri(SLOT_RESERVATION_URI, hubId)
                    .body(overlappingReserveSlotRequest)
                    .retrieve()
                    .toEntity(ReserveSlotResponse.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var errorDetails = exception.getResponseBodyAs(ErrorDetails.class);
        assertThat(errorDetails).isNotNull();
    }

    @Test
    void returns409_afterOnlyTenMinutesFromLastBooking() {
        hubId = gateTestDataFactory.seedHub();
        gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();

        var firstReserveSlotRequest = gateTestDataFactory.requestFor(gateId, routeId,
                "2026-09-01T11:00:00Z", "2026-09-01T11:45:00Z",
                TransportType.TRUCK, TemperatureMode.DRY);
        var overlappingReserveSlotRequest = gateTestDataFactory.requestFor(gateId, routeId,
                "2026-09-01T11:45:00Z", "2026-09-01T12:15:00Z",
                TransportType.TRUCK, TemperatureMode.DRY);

        restClient.post()
            .uri(SLOT_RESERVATION_URI, hubId)
            .body(firstReserveSlotRequest)
            .retrieve()
            .toEntity(ReserveSlotResponse.class);
        HttpClientErrorException exception = (HttpClientErrorException) catchException(
                () -> restClient.post()
                    .uri(SLOT_RESERVATION_URI, hubId)
                    .body(overlappingReserveSlotRequest)
                    .retrieve()
                    .toEntity(ReserveSlotResponse.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var errorDetails = exception.getResponseBodyAs(ErrorDetails.class);
        assertThat(errorDetails).isNotNull();
    }

    @Test
    void returns409_TenMinutesBeforeNextBooking() {
        hubId = gateTestDataFactory.seedHub();
        gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();

        var overlappingReserveSlotRequest = gateTestDataFactory.requestFor(gateId, routeId,
                "2026-09-01T11:05:00Z", "2026-09-01T11:50:00Z",
                TransportType.TRUCK, TemperatureMode.DRY);
        var secondReserveSlotRequest = gateTestDataFactory.requestFor(gateId, routeId,
                "2026-09-01T12:00:00Z", "2026-09-01T12:45:00Z",
                TransportType.TRUCK, TemperatureMode.DRY);

        restClient.post()
            .uri(SLOT_RESERVATION_URI, hubId)
            .body(secondReserveSlotRequest)
            .retrieve()
            .toEntity(ReserveSlotResponse.class);
        var exception = (HttpClientErrorException) catchException(
                () -> restClient.post()
                    .uri(SLOT_RESERVATION_URI, hubId)
                    .body(overlappingReserveSlotRequest)
                    .retrieve()
                    .toEntity(ReserveSlotResponse.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var errorDetails = exception.getResponseBodyAs(ErrorDetails.class);
        assertThat(errorDetails).isNotNull();
    }

    @Test
    void returns404_whenGateDoesNotBelongToHub() {
        hubId = gateTestDataFactory.seedHub();
        var otherHubsGateId = gateTestDataFactory.seedGate(gateTestDataFactory.seedHub(),
                TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();

        var reserveSlotRequest = gateTestDataFactory.requestFor(otherHubsGateId, routeId,
                "2026-09-01T09:00:00Z", "2026-09-01T09:45:00Z",
                TransportType.TRUCK, TemperatureMode.DRY);

        var exception = (HttpClientErrorException) catchException(
                () -> restClient.post()
                    .uri(SLOT_RESERVATION_URI, hubId)
                    .body(reserveSlotRequest)
                    .retrieve()
                    .toEntity(ReserveSlotResponse.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        var errorDetails = exception.getResponseBodyAs(ErrorDetails.class);
        assertThat(errorDetails).isNotNull();
    }

    @Test
    void returns422_whenGateCannotHandleFrozenCargo() {
        hubId = gateTestDataFactory.seedHub();
        var dryGateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY,
                TransportType.TRUCK);

        var frozenReserveSlotRequest = gateTestDataFactory.requestFor(dryGateId, routeId,
                "2026-09-01T13:00:00Z", "2026-09-01T13:45:00Z",
                TransportType.TRUCK, TemperatureMode.FROZEN);

        var exception = (HttpClientErrorException) catchException(
                () -> restClient.post()
                    .uri(SLOT_RESERVATION_URI, hubId)
                    .body(frozenReserveSlotRequest)
                    .retrieve()
                    .toEntity(ReserveSlotResponse.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        var errorDetails = exception.getResponseBodyAs(ErrorDetails.class);
        assertThat(errorDetails).isNotNull();
    }

    @Test
    void exactlyOneRequestSucceeds_underConcurrentDoubleBooking() throws Exception {
        final int concurrentCallsCounter = 20;
        final int startCallsCounter = 1;
        hubId = gateTestDataFactory.seedHub();
        gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();

        var reserveSlotRequest = gateTestDataFactory.requestFor(gateId, routeId,
                "2026-09-01T15:00:00Z", "2026-09-01T15:45:00Z",
                TransportType.TRUCK, TemperatureMode.DRY);

        var readyLatch = new CountDownLatch(concurrentCallsCounter);
        var startLatch = new CountDownLatch(startCallsCounter);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futureHttpStatusCodes = new ArrayList<Future<HttpStatusCode>>();
            for (int i = 0; i < concurrentCallsCounter; i++) {
                futureHttpStatusCodes.add(
                        executor.submit(
                                () -> {
                                    readyLatch.countDown();
                                    startLatch.await();

                                    try {
                                        var response = restClient.post()
                                            .uri(SLOT_RESERVATION_URI, hubId)
                                            .body(reserveSlotRequest)
                                            .retrieve()
                                            .toEntity(ReserveSlotResponse.class);
                                        return response.getStatusCode();
                                    } catch (HttpClientErrorException e) {
                                        return e.getStatusCode();
                                    }
                                }));
            }
            readyLatch.await();
            startLatch.countDown();

            long created = 0;
            long conflicted = 0;
            for (var future : futureHttpStatusCodes) {
                var status = future.get(10, TimeUnit.SECONDS);
                if (status == HttpStatus.CREATED) {
                    created++;
                } else if (status == HttpStatus.CONFLICT) {
                    conflicted++;
                }
            }

            assertThat(created).isEqualTo(1);
            assertThat(conflicted).isEqualTo(concurrentCallsCounter - 1);
        }
    }
}
