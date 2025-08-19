package com.yourticketing.concert_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Outbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;        // WATCHLIST_RESTOCK
    private Long aggregateId;   // concert id
    private int token;          // restockToken snapshot

    @Column(columnDefinition = "TEXT")
    private String payload;     // keep JSON as text for MVP

    private String status;      // PENDING | SENT
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}
