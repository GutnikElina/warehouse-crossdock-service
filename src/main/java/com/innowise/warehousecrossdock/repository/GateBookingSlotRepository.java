package com.innowise.warehousecrossdock.repository;

import com.innowise.warehousecrossdock.model.GateBookingSlot;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GateBookingSlotRepository extends JpaRepository<GateBookingSlot, UUID> {

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM gate_booking_slots
                WHERE gate_id = :gateId
                  AND booking_interval && tstzrange(:startTime, :endTime)
                  AND status <> 'CANCELLED'
                FOR UPDATE
            )""", nativeQuery = true)
    boolean existsOverlapping(
            @Param("gateId") UUID gateId,
            @Param("startTime") ZonedDateTime startTime,
            @Param("endTime") ZonedDateTime endTime);
}
