package com.yourticketing.concert_backend.controller;

import com.yourticketing.concert_backend.dto.ReserveDtos.ReserveRequest;
import com.yourticketing.concert_backend.dto.ReserveDtos.ReserveResponse;
import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import com.yourticketing.concert_backend.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/concerts")
public class ConcertController {

    private final ConcertRepository concertRepo;
    private final ReservationService reservationService;

    public ConcertController(ConcertRepository concertRepo,
                             ReservationService reservationService) {
        this.concertRepo = concertRepo;
        this.reservationService = reservationService;
    }

    @Operation(summary = "List all concerts with availability")
    @GetMapping
    public List<Concert> list() {
        return concertRepo.findAll();
    }

    @Operation(summary = "Reserve 1–6 tickets for a concert (120s hold)")
    @PostMapping("/{id}/reserve")
    public ResponseEntity<ReserveResponse> reserve(@PathVariable("id") Long concertId,
                                                   @Valid @RequestBody ReserveRequest req) {
        // Ensure the URL and body refer to the same concert
        req.concertId = concertId;
        return ResponseEntity.ok(reservationService.reserve(req));
    }
}

