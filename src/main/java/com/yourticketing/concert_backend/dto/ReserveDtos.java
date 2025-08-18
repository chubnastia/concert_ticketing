package com.yourticketing.concert_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ReserveDtos {

    public static class ReserveRequest {
        // set from the path in the controller; DO NOT annotate with @NotNull here
        public Long concertId;

        @NotBlank
        public String userId;

        @Min(1) @Max(6)
        public int quantity;
    }

    public static class ReserveResponse {
        public long reservationId;
        public ReserveResponse(long reservationId) { this.reservationId = reservationId; }
    }
}
