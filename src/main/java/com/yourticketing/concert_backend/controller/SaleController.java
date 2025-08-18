package com.yourticketing.concert_backend.controller;

import com.yourticketing.concert_backend.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sales")
public class SaleController {

    private final RefundService refundService;

    public SaleController(RefundService refundService) {
        this.refundService = refundService;
    }

    @Operation(summary = "Cancel a purchase (refund) and return tickets to availability")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable("id") long saleId) {
        refundService.cancelPurchase(saleId);
        return ResponseEntity.noContent().build(); // 204
    }
}
