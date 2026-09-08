package com.innowise.warehousecrossdock.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "warehouse_hubs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseHub {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "working_hours_start", nullable = false)
    private LocalTime workingHoursStart;

    @Column(name = "working_hours_end", nullable = false)
    private LocalTime workingHoursEnd;

    public ZoneId getZoneId() {
        return ZoneId.of(this.timezone);
    }
}