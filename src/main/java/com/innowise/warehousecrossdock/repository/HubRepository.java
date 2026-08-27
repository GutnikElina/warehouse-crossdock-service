package com.innowise.warehousecrossdock.repository;

import com.innowise.warehousecrossdock.model.WarehouseHub;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HubRepository extends JpaRepository<WarehouseHub, UUID> {}
