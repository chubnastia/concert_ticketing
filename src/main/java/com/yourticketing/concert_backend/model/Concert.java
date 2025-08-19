package com.yourticketing.concert_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
public class Concert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private LocalDateTime startTime;
    private String venue;

    private int capacity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private int availableTickets;

    @Version
    private int version;

    @Column(nullable = false)
    private boolean soldOut;

    @Column(nullable = false)
    private long restockToken;
}
