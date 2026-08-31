package com.innowise.warehousecrossdock.dto;

import com.esotericsoftware.kryo.serializers.FieldSerializer.NotNull;
import com.innowise.warehousecrossdock.model.TemperatureMode;
import com.innowise.warehousecrossdock.model.TransportType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReserveSlotRequest(
    @NotNull UUID gateId,
    @NotNull UUID routeId,
    @NotNull OffsetDateTime startTime,
    @NotNull OffsetDateTime endTime,
    @NotNull TransportType transportType,
    TemperatureMode temperatureMode) {
  public TemperatureMode requiredTemperatureMode() {
    return temperatureMode == null ? TemperatureMode.DRY : temperatureMode;
  }
}
