package com.innowise.warehousecrossdock.controller;

import com.innowise.warehousecrossdock.dto.AvailableGateSlotsResponse;
import com.innowise.warehousecrossdock.dto.ReserveSlotRequest;
import com.innowise.warehousecrossdock.dto.ReserveSlotResponse;
import com.innowise.warehousecrossdock.dto.SearchAvailableSlotsRequest;
import java.util.List;
import com.innowise.warehousecrossdock.service.impl.GateBookingServiceImpl;
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

    private final GateBookingServiceImpl gateBookingService;

    @PostMapping("/{hubId}/slots/reserve")
    public ResponseEntity<ReserveSlotResponse> reserveSlot(
            @PathVariable UUID hubId,
            @Valid @RequestBody ReserveSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(gateBookingService.reserveSlot(hubId, request));
    }

    @PostMapping("/{hubId}/slots/search")
    public ResponseEntity<List<AvailableGateSlotsResponse>> searchAvailableSlots(
            @PathVariable UUID hubId,
            @Valid @RequestBody SearchAvailableSlotsRequest request) {
        return ResponseEntity.ok(gateBookingService.searchAvailableSlots(hubId, request));
    }
}
