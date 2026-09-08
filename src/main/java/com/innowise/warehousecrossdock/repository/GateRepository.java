package com.innowise.warehousecrossdock.repository;

import com.innowise.warehousecrossdock.model.DockGate;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import com.innowise.warehousecrossdock.model.TransportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GateRepository extends JpaRepository<DockGate, UUID> {
    Optional<DockGate> findByIdAndHubId(UUID id, UUID hubId);

    @Query("SELECT g FROM DockGate g WHERE g.hubId = :hubId AND g.transportType = :transportType")
    List<DockGate> findByHubIdAndCriteria(
            @Param("hubId") UUID hubId,
            @Param("transportType") TransportType transportType);
}
