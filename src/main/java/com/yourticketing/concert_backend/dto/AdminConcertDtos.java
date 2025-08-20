package com.yourticketing.concert_backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AdminConcertDtos {
    private AdminConcertDtos() {}

    public record CreateRequest(
            @NotBlank(message = "Name is required")
            @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
            String name,

            @NotNull(message = "Start time is required")
            @Future(message = "Start time must be in the future")
            LocalDateTime startTime,

            @NotBlank(message = "Venue is required")
            @Size(min = 1, max = 255, message = "Venue must be between 1 and 255 characters")
            String venue,

            @Min(value = 0, message = "Capacity cannot be negative")
            @Max(value = 100000, message = "Capacity cannot exceed 100,000")
            int capacity,

            @DecimalMin(value = "0.0", message = "Price cannot be negative")
            @DecimalMax(value = "9999999.99", message = "Price too high")
            @Digits(integer = 7, fraction = 2, message = "Price format invalid")
            BigDecimal price
    ) {}

    public record UpdateRequest(
            @NotBlank(message = "Name is required")
            @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
            String name,

            @NotNull(message = "Start time is required")
            LocalDateTime startTime,

            @NotBlank(message = "Venue is required")
            @Size(min = 1, max = 255, message = "Venue must be between 1 and 255 characters")
            String venue,

            @Min(value = 0, message = "Capacity cannot be negative")
            @Max(value = 100000, message = "Capacity cannot exceed 100,000")
            int capacity,

            @DecimalMin(value = "0.0", message = "Price cannot be negative")
            @DecimalMax(value = "9999999.99", message = "Price too high")
            @Digits(integer = 7, fraction = 2, message = "Price format invalid")
            BigDecimal price
    ) {}
}