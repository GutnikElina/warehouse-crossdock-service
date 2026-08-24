package com.innowise.warehousecrossdock.repository;

import com.innowise.warehousecrossdock.model.DockGate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GateRepository extends JpaRepository<DockGate, UUID> {
  Optional<DockGate> findByIdAndHubId(UUID id, UUID hubId);
}

