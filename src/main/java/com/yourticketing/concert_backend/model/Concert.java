package com.yourticketing.concert_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Concert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private LocalDateTime startTime;
    private String venue;
    private int capacity;
    private double price;
    private int availableTickets;

    @Version
    private int version;

    // NEW
    private boolean soldOut = true;
    private int restockToken = 0;
}
