package com.yourticketing.concert_backend.repository;

import com.yourticketing.concert_backend.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
