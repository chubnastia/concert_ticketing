package com.yourticketing.concert_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long concertId;
    private String userId;  // Fake UUID or string for now
    private int quantity;
    private LocalDateTime expiry;
    private String status;  // e.g., ACTIVE, EXPIRED, BOUGHT, CANCELLED
}