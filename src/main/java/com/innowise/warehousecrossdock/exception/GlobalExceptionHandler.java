package com.innowise.warehousecrossdock.exception;

import com.innowise.warehousecrossdock.constant.ExceptionMessage;

import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(GateSlotAlreadyLockedException.class)
  public ResponseEntity<ErrorDetails> handleLocked(GateSlotAlreadyLockedException e) {
    HttpStatus conflict = HttpStatus.CONFLICT;

    var exception =
        ErrorDetails.builder()
            .message(e.getMessage())
            .errorName(conflict.getReasonPhrase())
            .httpStatus(conflict.value())
            .timestamp(Instant.now())
            .build();

    return new ResponseEntity<>(exception, conflict);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorDetails> handleDataIntegrityViolation(
      DataIntegrityViolationException e) {
    HttpStatus conflict = HttpStatus.CONFLICT;

    var exception =
        ErrorDetails.builder()
            .message(ExceptionMessage.DATA_INTEGRITY_VIOLATION_EXCEPTION)
            .errorName(conflict.getReasonPhrase())
            .httpStatus(conflict.value())
            .timestamp(Instant.now())
            .build();

    return new ResponseEntity<>(exception, conflict);
  }

  @ExceptionHandler(GateBookingInterruptedException.class)
  public ResponseEntity<ErrorDetails> handleInterrupt(GateBookingInterruptedException e) {
    HttpStatus serverError = HttpStatus.INTERNAL_SERVER_ERROR;

    var exception =
        ErrorDetails.builder()
            .message(e.getMessage())
            .errorName(serverError.getReasonPhrase())
            .httpStatus(serverError.value())
            .timestamp(Instant.now())
            .build();

    return new ResponseEntity<>(exception, serverError);
  }

  @ExceptionHandler(GateNotFoundException.class)
  public ResponseEntity<ErrorDetails> handleGateNotFound(GateNotFoundException e) {
    HttpStatus notFound = HttpStatus.NOT_FOUND;

    var exception =
        ErrorDetails.builder()
            .message(e.getMessage())
            .errorName(notFound.getReasonPhrase())
            .httpStatus(notFound.value())
            .timestamp(Instant.now())
            .build();

    return new ResponseEntity<>(exception, notFound);
  }

  @ExceptionHandler(SlotAlreadyBookedException.class)
  public ResponseEntity<ErrorDetails> handleSlotAlreadyBooked(SlotAlreadyBookedException e) {
    HttpStatus conflict = HttpStatus.CONFLICT;

    var exception =
        ErrorDetails.builder()
            .message(e.getMessage())
            .errorName(conflict.getReasonPhrase())
            .httpStatus(conflict.value())
            .timestamp(Instant.now())
            .build();

    return new ResponseEntity<>(exception, conflict);
  }

  @ExceptionHandler(IncompatibleGateException.class)
  public ResponseEntity<ErrorDetails> handleIncompatibleGate(IncompatibleGateException e) {
    HttpStatus unprocessable = HttpStatus.UNPROCESSABLE_ENTITY;

    var exception =
        ErrorDetails.builder()
            .message(e.getMessage())
            .errorName(unprocessable.getReasonPhrase())
            .httpStatus(unprocessable.value())
            .timestamp(Instant.now())
            .build();

    return new ResponseEntity<>(exception, unprocessable);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorDetails> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e) {

    HttpStatus badRequest = HttpStatus.BAD_REQUEST;

    var message =
        e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));

    var exception =
        ErrorDetails.builder()
            .message(message)
            .errorName(badRequest.getReasonPhrase())
            .httpStatus(badRequest.value())
            .timestamp(Instant.now())
            .build();

    return new ResponseEntity<>(exception, badRequest);
  }
}
