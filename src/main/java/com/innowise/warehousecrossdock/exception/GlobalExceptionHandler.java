package com.innowise.warehousecrossdock.exception;

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
}
