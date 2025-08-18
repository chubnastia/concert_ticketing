package com.yourticketing.concert_backend.repository;

import com.yourticketing.concert_backend.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    Optional<Sale> findByReservationId(Long reservationId);
}
