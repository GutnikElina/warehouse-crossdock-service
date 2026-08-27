package com.innowise.warehousecrossdock.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.exception.ErrorDetails;
import com.innowise.warehousecrossdock.model.GateBookingStatus;
import com.innowise.warehousecrossdock.model.TemperatureMode;
import com.innowise.warehousecrossdock.model.TransportType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GateBookingReservationIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16").withDatabaseName("crossdock");

  @Container
  static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
  }

  @Autowired TestRestTemplate restTemplate;

  @Autowired GateTestDataFactory gateTestDataFactory;

  private UUID hubId;
  private UUID gateId;
  private UUID routeId;

  @Test
  void returns201_andPersistsSlot_onFirstReservation() {
    hubId = gateTestDataFactory.seedHub();
    gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
    routeId = gateTestDataFactory.seedRoute();
    ReserveSlotRequest request =
        gateTestDataFactory.requestFor(
            gateId,
            routeId,
            "2026-09-01T10:00:00Z",
            "2026-09-01T10:45:00Z",
            TransportType.TRUCK,
            TemperatureMode.DRY);

    ResponseEntity<ReserveSlotResponse> response =
        restTemplate.postForEntity(
            "/api/v1/hubs/{hubId}/slots/reserve", request, ReserveSlotResponse.class, hubId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().status()).isEqualTo(GateBookingStatus.BOOKED);
  }

  @Test
  void returns201_afterFifteenMinuteGap() {
    hubId = gateTestDataFactory.seedHub();
    gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
    routeId = gateTestDataFactory.seedRoute();
    ReserveSlotRequest first =
        gateTestDataFactory.requestFor(
            gateId,
            routeId,
            "2026-09-01T11:00:00Z",
            "2026-09-01T11:45:00Z",
            TransportType.TRUCK,
            TemperatureMode.DRY);
    ReserveSlotRequest accepted =
        gateTestDataFactory.requestFor(
            gateId,
            routeId,
            "2026-09-01T12:00:00Z",
            "2026-09-01T12:45:00Z",
            TransportType.TRUCK,
            TemperatureMode.DRY);

    restTemplate.postForEntity(
        "/api/v1/hubs/{hubId}/slots/reserve", first, ReserveSlotResponse.class, hubId);
    ResponseEntity<ReserveSlotResponse> second =
        restTemplate.postForEntity(
            "/api/v1/hubs/{hubId}/slots/reserve", accepted, ReserveSlotResponse.class, hubId);

    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(second.getBody().status()).isEqualTo(GateBookingStatus.BOOKED);
  }

  @Test
  void returns409_onSecondOverlappingReservation() {
    hubId = gateTestDataFactory.seedHub();
    gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
    routeId = gateTestDataFactory.seedRoute();
    ReserveSlotRequest first =
        gateTestDataFactory.requestFor(
            gateId,
            routeId,
            "2026-09-01T11:00:00Z",
            "2026-09-01T11:45:00Z",
            TransportType.TRUCK,
            TemperatureMode.DRY);
    ReserveSlotRequest overlapping =
        gateTestDataFactory.requestFor(
            gateId,
            routeId,
            "2026-09-01T11:55:00Z",
            "2026-09-01T12:15:00Z",
            TransportType.TRUCK,
            TemperatureMode.DRY);

    restTemplate.postForEntity(
        "/api/v1/hubs/{hubId}/slots/reserve", first, ReserveSlotResponse.class, hubId);
    ResponseEntity<ErrorDetails> second =
        restTemplate.postForEntity(
            "/api/v1/hubs/{hubId}/slots/reserve", overlapping, ErrorDetails.class, hubId);

    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void returns409_afterOnlyTenMinutesFromLastBooking() {
    hubId = gateTestDataFactory.seedHub();
    gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
    routeId = gateTestDataFactory.seedRoute();
    ReserveSlotRequest first =
        gateTestDataFactory.requestFor(
            gateId,
            routeId,
            "2026-09-01T11:00:00Z",
            "2026-09-01T11:45:00Z",
            TransportType.TRUCK,
            TemperatureMode.DRY);
    ReserveSlotRequest overlapping =
        gateTestDataFactory.requestFor(
            gateId,
            routeId,
            "2026-09-01T11:45:00Z",
            "2026-09-01T12:15:00Z",
            TransportType.TRUCK,
            TemperatureMode.DRY);

    restTemplate.postForEntity(
        "/api/v1/hubs/{hubId}/slots/reserve", first, ReserveSlotResponse.class, hubId);
    ResponseEntity<ErrorDetails> second =
        restTemplate.postForEntity(
            "/api/v1/hubs/{hubId}/slots/reserve", overlapping, ErrorDetails.class, hubId);

    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void returns409_TenMinutesBeforeNextBooking() {
    hubId = gateTestDataFactory.seedHub();
    gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
    routeId = gateTestDataFactory.seedRoute();
    ReserveSlotRequest overlapping =
        gateTestDataFactory.requestFor(
            gateId,
            routeId,
            "2026-09-01T11:05:00Z",
            "2026-09-01T11:50:00Z",
            TransportType.TRUCK,
            TemperatureMode.DRY);
    ReserveSlotRequest second =
        gateTestDataFactory.requestFor(
            gateId,
            routeId,
            "2026-09-01T12:00:00Z",
            "2026-09-01T12:45:00Z",
            TransportType.TRUCK,
            TemperatureMode.DRY);

    restTemplate.postForEntity(
        "/api/v1/hubs/{hubId}/slots/reserve", second, ReserveSlotResponse.class, hubId);
    ResponseEntity<ErrorDetails> first =
        restTemplate.postForEntity(
            "/api/v1/hubs/{hubId}/slots/reserve", overlapping, ErrorDetails.class, hubId);

    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void returns404_whenGateDoesNotBelongToHub() {
    hubId = gateTestDataFactory.seedHub();
    UUID otherHubsGateId =
        gateTestDataFactory.seedGate(
            gateTestDataFactory.seedHub(), TemperatureMode.DRY, TransportType.TRUCK);
    UUID routeId = gateTestDataFactory.seedRoute();
    ReserveSlotRequest request =
        gateTestDataFactory.requestFor(
            otherHubsGateId,
            routeId,
            "2026-09-01T09:00:00Z",
            "2026-09-01T09:45:00Z",
            TransportType.TRUCK,
            TemperatureMode.DRY);

    ResponseEntity<ErrorDetails> response =
        restTemplate.postForEntity(
            "/api/v1/hubs/{hubId}/slots/reserve", request, ErrorDetails.class, hubId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void returns422_whenGateCannotHandleFrozenCargo() {
    hubId = gateTestDataFactory.seedHub();
    UUID dryGateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);

    ReserveSlotRequest frozenCargoRequest =
        gateTestDataFactory.requestFor(
            dryGateId,
            routeId,
            "2026-09-01T13:00:00Z",
            "2026-09-01T13:45:00Z",
            TransportType.TRUCK,
            TemperatureMode.FROZEN);

    ResponseEntity<ErrorDetails> response =
        restTemplate.postForEntity(
            "/api/v1/hubs/{hubId}/slots/reserve", frozenCargoRequest, ErrorDetails.class, hubId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @Test
  void exactlyOneRequestSucceeds_underConcurrentDoubleBooking() throws Exception {
    hubId = gateTestDataFactory.seedHub();
    gateId = gateTestDataFactory.seedGate(hubId, TemperatureMode.DRY, TransportType.TRUCK);
    routeId = gateTestDataFactory.seedRoute();
    ReserveSlotRequest request =
        gateTestDataFactory.requestFor(
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
                  ResponseEntity<ErrorDetails> resp =
                      restTemplate.postForEntity(
                          "/api/v1/hubs/{hubId}/slots/reserve", request, ErrorDetails.class, hubId);
                  return resp.getStatusCode();
                }));
      }
      readyLatch.await();
      startLatch.countDown();

      long created = 0;
      long conflicted = 0;
      for (Future<HttpStatusCode> future : futures) {
        HttpStatusCode status = future.get(10, TimeUnit.SECONDS);
        if (status == HttpStatus.CREATED) created++;
        else if (status == HttpStatus.CONFLICT) conflicted++;
      }

      assertThat(created).isEqualTo(1);
      assertThat(conflicted).isEqualTo(concurrentCalls - 1);
    }
  }
}
