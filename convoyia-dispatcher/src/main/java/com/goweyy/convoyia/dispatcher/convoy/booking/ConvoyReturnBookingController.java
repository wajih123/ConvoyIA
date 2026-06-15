package com.goweyy.convoyia.dispatcher.convoy.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/convoy/book")
@RequiredArgsConstructor
public class ConvoyReturnBookingController {

    private final ConvoyReturnBookingService returnBookingService;

    @PostMapping("/{missionId}/return")
    public ResponseEntity<ConvoyReturnBookingResult> bookReturn(@PathVariable String missionId,
                                                                @RequestBody ConvoyReturnBookingRequest request) {
        return ResponseEntity.ok(returnBookingService.bookReturn(missionId, request.getArrivalAddress(), request.getConveyorHomeCity()));
    }
}
