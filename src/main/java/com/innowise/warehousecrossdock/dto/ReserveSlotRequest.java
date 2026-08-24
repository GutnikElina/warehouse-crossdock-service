package com.innowise.warehousecrossdock.dto;

import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.innowise.warehousecrossdock.model.TemperatureMode;
import com.innowise.warehousecrossdock.model.TransportType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReserveSlotRequest(
        @FieldSerializer.NotNull UUID gateId,
        @FieldSerializer.NotNull UUID routeId,
        @FieldSerializer.NotNull OffsetDateTime startTime,
        @FieldSerializer.NotNull OffsetDateTime endTime,
        @FieldSerializer.NotNull TransportType transportType,
        TemperatureMode temperatureMode
) {
  public TemperatureMode requiredTemperatureMode() {
    return temperatureMode == null ? TemperatureMode.DRY : temperatureMode;
  }

}
