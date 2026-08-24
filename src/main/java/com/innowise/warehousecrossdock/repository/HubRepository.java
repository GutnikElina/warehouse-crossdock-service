package com.innowise.warehousecrossdock.repository;

import com.innowise.warehousecrossdock.model.WarehouseHub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HubRepository extends JpaRepository<WarehouseHub, UUID>{
}
