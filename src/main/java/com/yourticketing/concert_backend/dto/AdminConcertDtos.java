package com.yourticketing.concert_backend.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class AdminConcertDtos {

    private AdminConcertDtos() {}

    public static class CreateRequest {
        @NotBlank public String name;
        @NotNull public LocalDateTime startTime; // ISO-8601: 2025-09-01T20:00:00
        @NotBlank public String venue;
        @Min(0) public int capacity;
        @PositiveOrZero public double price;
    }

    public static class UpdateRequest {
        @NotBlank public String name;
        @NotNull public LocalDateTime startTime;
        @NotBlank public String venue;
        @Min(0) public int capacity;
        @PositiveOrZero public double price;
    }
}
