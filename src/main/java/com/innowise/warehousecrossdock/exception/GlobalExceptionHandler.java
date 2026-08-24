package com.innowise.warehousecrossdock.exception;

import com.innowise.warehousecrossdock.constant.ExceptionMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(GateSlotAlreadyLockedException.class)
    public ResponseEntity<ErrorDetails> handleLocked(GateSlotAlreadyLockedException e){
        HttpStatus conflict = HttpStatus.CONFLICT;

        ErrorDetails exception = ErrorDetails.builder()
                .message(e.getMessage())
                .errorName(conflict.getReasonPhrase())
                .httpStatus(conflict.value())
                .timestamp(LocalDateTime.now())
                .build();

        return  new ResponseEntity<>(exception, conflict);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDetails> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        HttpStatus conflict = HttpStatus.CONFLICT;

        ErrorDetails exception = ErrorDetails.builder()
                .message(ExceptionMessage.DATA_INTEGRITY_VIOLATION_EXCEPTION)
                .errorName(conflict.getReasonPhrase())
                .httpStatus(conflict.value())
                .timestamp(LocalDateTime.now())
                .build();

        return  new ResponseEntity<>(exception, conflict);
    }

    @ExceptionHandler(GateBookingInterruptedException.class)
    public ResponseEntity<ErrorDetails> handleInterrupt(GateBookingInterruptedException e){
        HttpStatus serverError = HttpStatus.INTERNAL_SERVER_ERROR;

        ErrorDetails exception = ErrorDetails.builder()
                .message(e.getMessage())
                .errorName(serverError.getReasonPhrase())
                .httpStatus(serverError.value())
                .timestamp(LocalDateTime.now())
                .build();

        return  new ResponseEntity<>(exception, serverError);
    }

    @ExceptionHandler(GateNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleGateNotFound(GateNotFoundException e) {
        HttpStatus notFound = HttpStatus.NOT_FOUND;

        ErrorDetails exception = ErrorDetails.builder()
                .message(e.getMessage())
                .errorName(notFound.getReasonPhrase())
                .httpStatus(notFound.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(exception, notFound);
    }

    @ExceptionHandler(SlotAlreadyBookedException.class)
    public ResponseEntity<ErrorDetails> handleSlotAlreadyBooked(SlotAlreadyBookedException e) {
        HttpStatus conflict = HttpStatus.CONFLICT;

        ErrorDetails exception = ErrorDetails.builder()
                .message(e.getMessage())
                .errorName(conflict.getReasonPhrase())
                .httpStatus(conflict.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(exception, conflict);
    }

    @ExceptionHandler(IncompatibleGateException.class)
    public ResponseEntity<ErrorDetails> handleIncompatibleGate(IncompatibleGateException e) {
        HttpStatus unprocessable = HttpStatus.UNPROCESSABLE_ENTITY;

        ErrorDetails exception = ErrorDetails.builder()
                .message(e.getMessage())
                .errorName(unprocessable.getReasonPhrase())
                .httpStatus(unprocessable.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(exception, unprocessable);
    }
}
