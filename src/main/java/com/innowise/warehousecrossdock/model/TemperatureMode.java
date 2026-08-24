package com.innowise.warehousecrossdock.model;

public enum TemperatureMode {
  DRY, CHILLED, FROZEN;

  public boolean supports(TemperatureMode required) {
    return this.ordinal() >= required.ordinal();
  }
}

