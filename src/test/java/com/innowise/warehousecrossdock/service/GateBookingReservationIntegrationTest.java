package com.innowise.warehousecrossdock.service;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;

class GateBookingReservationIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @Autowired
    GateTestDataFactory gateTestDataFactory;

    private UUID hubId;
    private UUID gateId;
    private UUID routeId;

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
        var request = gateTestDataFactory.requestFor(
                gateId,
                routeId,
                "2026-09-01T10:00:00Z",
                "2026-09-01T10:45:00Z",
                TransportType.TRUCK,
                TemperatureMode.DRY);

        var responseEntity = restClient
            .post()
            .uri("/api/v1/hubs/{hubId}/slots/reserve", hubId)
            .body(request)
            .retrieve()
            .toEntity(ReserveSlotResponse.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().status()).isEqualTo(GateBookingStatus.BOOKED);
    }

    @Test
    void returns201_afterFifteenMinuteGap() {
        hubId = gateTestDataFactory.seedHub();
        gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();
        var first = gateTestDataFactory.requestFor( // TODO rename
                gateId,
                routeId,
                "2026-09-01T11:00:00Z",
                "2026-09-01T11:45:00Z",
                TransportType.TRUCK,
                TemperatureMode.DRY);
        var accepted = gateTestDataFactory.requestFor( // TODO rename
                gateId,
                routeId,
                "2026-09-01T12:00:00Z",
                "2026-09-01T12:45:00Z",
                TransportType.TRUCK,
                TemperatureMode.DRY);

        restClient.post()
            .uri("/api/v1/hubs/{hubId}/slots/reserve", hubId)
            .body(first)
            .retrieve()
            .toEntity(ReserveSlotResponse.class);
        var secondResultResponseEntity = restClient.post()
            .uri("/api/v1/hubs/{hubId}/slots/reserve", hubId)
            .body(accepted)
            .retrieve()
            .toEntity(ReserveSlotResponse.class);

        assertThat(secondResultResponseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondResultResponseEntity.getBody()).isNotNull();
        assertThat(secondResultResponseEntity.getBody().status())
            .isEqualTo(GateBookingStatus.BOOKED);
    }

    @Test
    void returns409_onSecondOverlappingReservation() {
        hubId = gateTestDataFactory.seedHub();
        gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();
        ReserveSlotRequest first = gateTestDataFactory.requestFor(
                gateId,
                routeId,
                "2026-09-01T11:00:00Z",
                "2026-09-01T11:45:00Z",
                TransportType.TRUCK,
                TemperatureMode.DRY);
        ReserveSlotRequest overlapping = gateTestDataFactory.requestFor(
                gateId,
                routeId,
                "2026-09-01T11:55:00Z",
                "2026-09-01T12:15:00Z",
                TransportType.TRUCK,
                TemperatureMode.DRY);

        restClient.post()
            .uri("/api/v1/hubs/{hubId}/slots/reserve", hubId)
            .body(first)
            .retrieve()
            .toEntity(ReserveSlotResponse.class);
        HttpClientErrorException exception = (HttpClientErrorException) catchException(
                () -> restClient.post()
                    .uri("/api/v1/hubs/{hubId}/slots/reserve", hubId)
                    .body(overlapping)
                    .retrieve()
                    .toEntity(ReserveSlotResponse.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorDetails errorDetails = exception.getResponseBodyAs(ErrorDetails.class);
        assertThat(errorDetails).isNotNull();
    }

    @Test
    void returns409_afterOnlyTenMinutesFromLastBooking() {
        hubId = gateTestDataFactory.seedHub();
        gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();
        ReserveSlotRequest first = gateTestDataFactory.requestFor(
                gateId,
                routeId,
                "2026-09-01T11:00:00Z",
                "2026-09-01T11:45:00Z",
                TransportType.TRUCK,
                TemperatureMode.DRY);
        ReserveSlotRequest overlapping = gateTestDataFactory.requestFor(
                gateId,
                routeId,
                "2026-09-01T11:45:00Z",
                "2026-09-01T12:15:00Z",
                TransportType.TRUCK,
                TemperatureMode.DRY);

        restClient.post()
            .uri("/api/v1/hubs/{hubId}/slots/reserve", hubId)
            .body(first)
            .retrieve()
            .toEntity(ReserveSlotResponse.class);
        HttpClientErrorException exception = (HttpClientErrorException) catchException(
                () -> restClient.post()
                    .uri("/api/v1/hubs/{hubId}/slots/reserve", hubId)
                    .body(overlapping)
                    .retrieve()
                    .toEntity(ReserveSlotResponse.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorDetails errorDetails = exception.getResponseBodyAs(ErrorDetails.class);
        assertThat(errorDetails).isNotNull();
    }

    @Test
    void returns409_TenMinutesBeforeNextBooking() {
        hubId = gateTestDataFactory.seedHub();
        gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();
        ReserveSlotRequest overlapping = gateTestDataFactory.requestFor(
                gateId,
                routeId,
                "2026-09-01T11:05:00Z",
                "2026-09-01T11:50:00Z",
                TransportType.TRUCK,
                TemperatureMode.DRY);
        ReserveSlotRequest second = gateTestDataFactory.requestFor(
                gateId,
                routeId,
                "2026-09-01T12:00:00Z",
                "2026-09-01T12:45:00Z",
                TransportType.TRUCK,
                TemperatureMode.DRY);

        restClient.post()
            .uri("/api/v1/hubs/{hubId}/slots/reserve", hubId)
            .body(second)
            .retrieve()
            .toEntity(ReserveSlotResponse.class);
        HttpClientErrorException exception = (HttpClientErrorException) catchException(
                () -> restClient.post()
                    .uri("/api/v1/hubs/{hubId}/slots/reserve", hubId)
                    .body(overlapping)
                    .retrieve()
                    .toEntity(ReserveSlotResponse.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorDetails errorDetails = exception.getResponseBodyAs(ErrorDetails.class);
        assertThat(errorDetails).isNotNull();
    }

    @Test
    void returns404_whenGateDoesNotBelongToHub() {
        hubId = gateTestDataFactory.seedHub();
        UUID otherHubsGateId = gateTestDataFactory.seedGate(
                gateTestDataFactory.seedHub(), TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();
        ReserveSlotRequest request = gateTestDataFactory.requestFor(
                otherHubsGateId,
                routeId,
                "2026-09-01T09:00:00Z",
                "2026-09-01T09:45:00Z",
                TransportType.TRUCK,
                TemperatureMode.DRY);

        HttpClientErrorException exception = (HttpClientErrorException) catchException(
                () -> restClient.post()
                    .uri("/api/v1/hubs/{hubId}/slots/reserve", hubId)
                    .body(request)
                    .retrieve()
                    .toEntity(ReserveSlotResponse.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorDetails errorDetails = exception.getResponseBodyAs(ErrorDetails.class);
        assertThat(errorDetails).isNotNull();
    }

    @Test
    void returns422_whenGateCannotHandleFrozenCargo() {
        hubId = gateTestDataFactory.seedHub();
        UUID dryGateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY,
                TransportType.TRUCK);

        ReserveSlotRequest frozenCargoRequest = gateTestDataFactory.requestFor(
                dryGateId,
                routeId,
                "2026-09-01T13:00:00Z",
                "2026-09-01T13:45:00Z",
                TransportType.TRUCK,
                TemperatureMode.FROZEN);

        HttpClientErrorException exception = (HttpClientErrorException) catchException(
                () -> restClient.post()
                    .uri("/api/v1/hubs/{hubId}/slots/reserve", hubId)
                    .body(frozenCargoRequest)
                    .retrieve()
                    .toEntity(ReserveSlotResponse.class));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        ErrorDetails errorDetails = exception.getResponseBodyAs(ErrorDetails.class);
        assertThat(errorDetails).isNotNull();
    }

    @Test
    void exactlyOneRequestSucceeds_underConcurrentDoubleBooking() throws Exception {
        hubId = gateTestDataFactory.seedHub();
        gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
        routeId = gateTestDataFactory.seedRoute();
        ReserveSlotRequest request = gateTestDataFactory.requestFor(
                gateId,
                routeId,
                "2026-09-01T15:00:00Z",
                "2026-09-01T15:45:00Z",
                TransportType.TRUCK,
                TemperatureMode.DRY);
        int concurrentCalls = 20;
        CountDownLatch readyLatch = new CountDownLatch(concurrentCalls);
        CountDownLatch startLatch = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<HttpStatusCode>> futures = new ArrayList<>();
            for (int i = 0; i < concurrentCalls; i++) {
                futures.add(
                        executor.submit(
                                () -> {
                                    readyLatch.countDown();
                                    startLatch.await();
                                    var exception = (HttpClientErrorException) catchException(
                                            () -> restClient.post()
                                                .uri("/api/v1/hubs/{hubId}/slots/reserve",
                                                        hubId)
                                                .body(request)
                                                .retrieve()
                                                .toEntity(ReserveSlotResponse.class));
                                    return exception.getStatusCode();
                                }));
            }
            readyLatch.await();
            startLatch.countDown();

            long created = 0;
            long conflicted = 0;
            for (Future<HttpStatusCode> future : futures) {
                HttpStatusCode status = future.get(10, TimeUnit.SECONDS);
                if (status == HttpStatus.CREATED)
                    created++;
                else if (status == HttpStatus.CONFLICT)
                    conflicted++;
            }

            assertThat(created).isEqualTo(1);
            assertThat(conflicted).isEqualTo(concurrentCalls - 1);
        }
    }
}
