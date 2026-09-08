package com.innowise.warehousecrossdock.repository;

import com.innowise.warehousecrossdock.model.GateBookingSlot;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GateBookingSlotRepository extends JpaRepository<GateBookingSlot, UUID> {

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM gate_booking_slots
                WHERE gate_id = :gateId
                AND booking_interval && tstzrange(:startTime, :endTime)
                AND status NOT IN ('CANCELLED', 'RELEASED')
                FOR UPDATE
            )""", nativeQuery = true)
    boolean existsOverlapping(
            @Param("gateId") UUID gateId,
            @Param("startTime") ZonedDateTime startTime,
            @Param("endTime") ZonedDateTime endTime);

    @Query(value = """
            SELECT * FROM gate_booking_slots
            WHERE gate_id IN :gateIds
            AND booking_interval && tstzrange(:startTime, :endTime)
            AND status NOT IN ('CANCELLED', 'RELEASED')
            ORDER BY gate_id, lower(booking_interval) ASC
            """, nativeQuery = true)
    List<GateBookingSlot> findOverlappingForGates(
            @Param("gateIds") List<UUID> gateIds,
            @Param("startTime") ZonedDateTime startTime,
            @Param("endTime") ZonedDateTime endTime);
}
