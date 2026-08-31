package com.innowise.warehousecrossdock.controller;

import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.service.GateBookingService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hubs")
@RequiredArgsConstructor
public class GateBookingController {

    private final GateBookingService gateBookingServiceImpl;

    @PostMapping("/{hubId}/slots/reserve")
    public ResponseEntity<ReserveSlotResponse> reserveSlot(
            @PathVariable UUID hubId, @Valid @RequestBody ReserveSlotRequest request) {

        ReserveSlotResponse response = gateBookingServiceImpl.reserveSlot(hubId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
