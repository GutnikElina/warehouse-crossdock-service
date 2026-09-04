package com.innowise.warehousecrossdock.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dock_gates")
public class DockGate {

    @Id
    private UUID id;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "temperature_mode", nullable = false)
    private TemperatureMode temperatureMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_type", nullable = false)
    private TransportType transportType;

    public boolean supports(TransportType type) {
        return transportType == type;
    }

    public boolean matchesTemperature(TemperatureMode required) {
        return temperatureMode.supports(required);
    }
}
