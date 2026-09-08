package com.innowise.warehousecrossdock.dto;

import com.innowise.warehousecrossdock.model.TemperatureMode;
import com.innowise.warehousecrossdock.model.TransportType;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.time.OffsetDateTime;

public record SearchAvailableSlotsRequest(
        @NotNull OffsetDateTime searchFrom,
        @NotNull OffsetDateTime searchTo,
        @NotNull TransportType transportType,
        TemperatureMode temperatureMode,
        @NotNull Duration requiredDuration) {

    public TemperatureMode requiredTemperatureMode() {
        return temperatureMode == null ? TemperatureMode.DRY : temperatureMode;
    }
}
