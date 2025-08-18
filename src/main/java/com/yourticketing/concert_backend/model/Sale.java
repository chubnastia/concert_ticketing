package com.yourticketing.concert_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "sale", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sale_reservation", columnNames = {"reservationId"})
})
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // one Sale per Reservation (enforces idempotent buy)
    @Column(nullable = false)
    private Long reservationId;

    // COMPLETED or REFUNDED
    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "COMPLETED";
    }
}
