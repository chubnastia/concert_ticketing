package com.yourticketing.concert_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AdminConcertDtos {
    private AdminConcertDtos() {}

    public record CreateRequest(
            String name,
            LocalDateTime startTime,
            String venue,
            int capacity,
            BigDecimal price
    ) {}

    public record UpdateRequest(
            String name,
            LocalDateTime startTime,
            String venue,
            int capacity,
            BigDecimal price
    ) {}
}
