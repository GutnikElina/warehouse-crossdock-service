package com.innowise.warehousecrossdock.service;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.model.DockGate;
import com.innowise.warehousecrossdock.model.TemperatureMode;
import com.innowise.warehousecrossdock.model.TransportType;
import com.innowise.warehousecrossdock.model.WarehouseHub;
import com.innowise.warehousecrossdock.repository.GateRepository;
import com.innowise.warehousecrossdock.repository.HubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class GateTestDataFactory {

  @Autowired private HubRepository hubRepository;
  @Autowired private GateRepository gateRepository;

  public UUID seedHub() {
    WarehouseHub tempHub = new WarehouseHub(UUID.randomUUID(), "Hub 1", "New York");
    return hubRepository.saveAndFlush(tempHub).getId();
  }

  public UUID seedGate(UUID hubId, TemperatureMode temperatureMode, TransportType transportType) {
    DockGate tempGate = new DockGate(UUID.randomUUID(), hubId, "Gate A1", temperatureMode, transportType);

    return gateRepository.saveAndFlush(tempGate).getId();
  }

  public UUID seedRoute() {
    return UUID.randomUUID();
  }

  public ReserveSlotRequest requestFor(UUID gateId, UUID routeId,
                                       String startIso, String endIso,
                                       TransportType transportType, TemperatureMode temperatureMode) {

    OffsetDateTime startTime = OffsetDateTime.parse(startIso);
    OffsetDateTime endTime = OffsetDateTime.parse(endIso);
    return new ReserveSlotRequest(gateId, routeId, startTime, endTime, transportType, temperatureMode);
  }
}
