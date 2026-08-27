package com.innowise.warehousecrossdock.model;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import io.hypersistence.utils.hibernate.type.range.Range;
import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import com.innowise.warehousecrossdock.constant.ConfigValues;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "gate_booking_slots")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GateBookingSlot {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "gate_id", nullable = false)
  private UUID gateId;

  @Column(name = "route_id", nullable = false)
  private UUID routeId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GateBookingStatus status;

  @Type(PostgreSQLRangeType.class)
  @Column(name = "booking_interval", nullable = false, columnDefinition = "tstzrange")
  private Range<ZonedDateTime> bookingInterval;

  public static GateBookingSlot book(ReserveSlotRequest request) {
    Range<ZonedDateTime> interval = Range.closedOpen(
            request.startTime().toZonedDateTime(),
            request.endTime().toZonedDateTime().plus(ConfigValues.SLOT_BOOKING_INTERVAL));
    return new GateBookingSlot(null, request.gateId(), request.routeId(), GateBookingStatus.BOOKED, interval);
  }
}

