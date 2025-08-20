package com.yourticketing.concert_backend.controller;

import com.yourticketing.concert_backend.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/concerts/{id}/watchlist")
public class WatchlistController {

    private final WatchlistService service;

    public WatchlistController(WatchlistService service) {
        this.service = service;
    }

    public static class JoinRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        public String email;

        // Default constructor for Jackson
        public JoinRequest() {}

        public JoinRequest(String email) {
            this.email = email;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    @Operation(summary = "Join watchlist when the concert is sold out")
    @PostMapping
    public ResponseEntity<Void> join(@PathVariable("id") Long concertId,
                                     @Valid @RequestBody JoinRequest req) {
        service.join(concertId, req.email);
        return ResponseEntity.accepted().build(); // 202
    }
}