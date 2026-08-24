package com.innowise.warehousecrossdock.exception;

import static com.innowise.warehousecrossdock.constant.ExceptionMessage.*;

public class SlotAlreadyBookedException extends RuntimeException {
  public SlotAlreadyBookedException() {
    super(DATA_INTEGRITY_VIOLATION_EXCEPTION);
  }
}

