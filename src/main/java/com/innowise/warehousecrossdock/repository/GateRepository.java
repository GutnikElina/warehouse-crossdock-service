package com.innowise.warehousecrossdock.repository;

import com.innowise.warehousecrossdock.model.DockGate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GateRepository extends JpaRepository<DockGate, UUID> {
  Optional<DockGate> findByIdAndHubId(UUID id, UUID hubId);
}
