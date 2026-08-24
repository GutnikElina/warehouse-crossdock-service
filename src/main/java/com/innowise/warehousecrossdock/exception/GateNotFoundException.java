package com.innowise.warehousecrossdock.exception;

import static com.innowise.warehousecrossdock.constant.ExceptionMessage.*;

public class GateNotFoundException extends RuntimeException {
  public GateNotFoundException() {
    super(GATE_NOT_FOUND_EXCEPTION_MESSAGE);
  }
}

