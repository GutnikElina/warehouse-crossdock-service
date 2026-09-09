package com.innowise.warehousecrossdock.exception;

import com.innowise.warehousecrossdock.constant.ExceptionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final HttpStatus CONFLICT = HttpStatus.CONFLICT;
    public static final HttpStatus NOT_FOUND = HttpStatus.NOT_FOUND;
    public static final HttpStatus BAD_REQUEST = HttpStatus.BAD_REQUEST;
    public static final HttpStatus UNPROCESSABLE_CONTENT = HttpStatus.UNPROCESSABLE_CONTENT;
    public static final HttpStatus INTERNAL_SERVER_ERROR = HttpStatus.INTERNAL_SERVER_ERROR;

    @ExceptionHandler(GateSlotAlreadyLockedException.class)
    public ResponseEntity<ErrorDetails> handleLocked(GateSlotAlreadyLockedException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(new ErrorDetails(e.getMessage(), Instant.now()), CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDetails> handleDataIntegrityViolation(
            DataIntegrityViolationException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(
                new ErrorDetails(ExceptionMessage.DATA_INTEGRITY_VIOLATION_EXCEPTION,
                        Instant.now()),
                CONFLICT);
    }

    @ExceptionHandler(GateBookingInterruptedException.class)
    public ResponseEntity<ErrorDetails> handleInterrupt(GateBookingInterruptedException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(new ErrorDetails(e.getMessage(), Instant.now()),
                INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(GateNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleGateNotFound(GateNotFoundException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(new ErrorDetails(e.getMessage(), Instant.now()), NOT_FOUND);
    }

    @ExceptionHandler(SlotAlreadyBookedException.class)
    public ResponseEntity<ErrorDetails> handleSlotAlreadyBooked(SlotAlreadyBookedException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(new ErrorDetails(e.getMessage(), Instant.now()), CONFLICT);
    }

    @ExceptionHandler(IncompatibleGateException.class)
    public ResponseEntity<ErrorDetails> handleIncompatibleGate(IncompatibleGateException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(new ErrorDetails(e.getMessage(), Instant.now()),
                UNPROCESSABLE_CONTENT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e) {
        var message = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        log.warn(message);
        return new ResponseEntity<>(new ErrorDetails(message, Instant.now()), BAD_REQUEST);
    }
}
