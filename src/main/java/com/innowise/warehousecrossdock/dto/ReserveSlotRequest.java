package com.innowise.warehousecrossdock.dto;

import com.esotericsoftware.kryo.serializers.FieldSerializer.NotNull;
import com.innowise.warehousecrossdock.model.TemperatureMode;
import com.innowise.warehousecrossdock.model.TransportType;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReserveSlotRequest(
        @Nullable UUID gateId,
        @NotNull UUID routeId,
        @NotNull OffsetDateTime startTime,
        @NotNull OffsetDateTime endTime,
        @NotNull TransportType transportType,
        TemperatureMode temperatureMode) {

    public TemperatureMode requiredTemperatureMode() {
        return temperatureMode == null ? TemperatureMode.DRY : temperatureMode;
    }

    public ReserveSlotRequest withGateId(UUID newGateId) {
        return new ReserveSlotRequest(newGateId, routeId, startTime, endTime, transportType,
                temperatureMode);
    }
}
