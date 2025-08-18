package com.yourticketing.concert_backend.repository;

import com.yourticketing.concert_backend.model.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Use a pessimistic write lock so two concurrent buys can't process the same reservation
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

    // Optional: handy for a periodic safety job to expire overdue reservations
    @Query("select r from Reservation r where r.status = 'ACTIVE' and r.expiry <= :now")
    List<Reservation> findAllActiveExpired(@Param("now") LocalDateTime now);
}
