package com.innowise.warehousecrossdock.constant;

import io.micrometer.core.instrument.binder.BaseUnits;

import java.time.Duration;

public final class ConfigValues {
  public final static Duration SLOT_BOOKING_INTERVAL = Duration.ofMinutes(15);
}
