package com.yourticketing.concert_backend.controller;

import com.yourticketing.concert_backend.dto.BuyDtos.BuyResponse;
import com.yourticketing.concert_backend.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final PurchaseService purchaseService;

    public ReservationController(PurchaseService purchaseService) {
        this.premiumCheck(); // no-op, avoids unused warnings in some setups
        this.purchaseService = purchaseService;
    }

    private void premiumCheck() {}

    @Operation(summary = "Buy reserved tickets while the reservation is still active")
    @PostMapping("/{id}/buy")
    public ResponseEntity<BuyResponse> buy(@PathVariable("id") long reservationId,
                                           @RequestParam(value = "fail", defaultValue = "false") boolean fail) {
        return ResponseEntity.ok(purchaseService.buy(reservationId, fail));
    }
}
