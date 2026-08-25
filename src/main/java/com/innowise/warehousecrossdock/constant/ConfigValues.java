package com.innowise.warehousecrossdock.constant;

import io.micrometer.core.instrument.binder.BaseUnits;

import java.time.Duration;

public final class ConfigValues {
  public final static Duration BUFFER_TIME = Duration.ofMinutes(15);
}
