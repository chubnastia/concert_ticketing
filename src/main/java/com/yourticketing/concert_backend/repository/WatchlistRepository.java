package com.yourticketing.concert_backend.repository;

import com.yourticketing.concert_backend.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByConcertId(Long concertId);

    // NEW: fast idempotency check
    boolean existsByConcertIdAndEmail(Long concertId, String email);
}
