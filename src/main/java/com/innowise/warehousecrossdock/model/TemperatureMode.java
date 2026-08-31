package com.innowise.warehousecrossdock.model;

public enum TemperatureMode {
    DRY(0), CHILLED(1), FROZEN(2);

    private final int level;

    TemperatureMode(int level) {
        this.level = level;
    }

    public boolean supports(TemperatureMode required) {
        return this.level >= required.level;
    }
}
