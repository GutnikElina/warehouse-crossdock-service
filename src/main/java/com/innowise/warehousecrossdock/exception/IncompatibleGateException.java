package com.innowise.warehousecrossdock.exception;

import static com.innowise.warehousecrossdock.constant.ExceptionMessage.*;

public class IncompatibleGateException extends RuntimeException {
  public IncompatibleGateException() {
    super(GATE_INCOMPATIBLE_EXCEPTION_MESSAGE);
  }
}

