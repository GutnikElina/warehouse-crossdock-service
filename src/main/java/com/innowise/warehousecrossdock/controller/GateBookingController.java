package com.innowise.warehousecrossdock.controller;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.service.GateBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hubs")
@RequiredArgsConstructor
public class GateBookingController {

  private final GateBookingService gateBookingService;

  @PostMapping("/{hubId}/slots/reserve")
  public ResponseEntity<ReserveSlotResponse> reserveSlot(
          @PathVariable UUID hubId,
          @Valid @RequestBody ReserveSlotRequest request) {

    ReserveSlotResponse response = gateBookingService.reserveSlot(hubId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}

