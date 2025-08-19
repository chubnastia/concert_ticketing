package com.yourticketing.concert_backend.service;

import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.model.Watchlist;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import com.yourticketing.concert_backend.repository.WatchlistRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class WatchlistService {

    private final ConcertRepository concertRepo;
    private final WatchlistRepository watchlistRepo;

    public WatchlistService(ConcertRepository concertRepo, WatchlistRepository watchlistRepo) {
        this.concertRepo = concertRepo;
        this.watchlistRepo = watchlistRepo;
    }

    @Transactional
    public void join(Long concertId, String email) {
        Concert c = concertRepo.findById(concertId).orElseThrow();

        if (c.getAvailableTickets() > 0) {
            // Per acceptance criteria: allow join only when sold out
            throw new IllegalStateException("watchlist allowed only when sold out");
        }

        String normalized = email.trim().toLowerCase();

        // Fast path: if already present, return (idempotent)
        if (watchlistRepo.existsByConcertIdAndEmail(concertId, normalized)) {
            return; // 202 from controller
        }

        Watchlist w = new Watchlist();
        w.setConcertId(concertId);
        w.setEmail(normalized);

        try {
            watchlistRepo.save(w);
        } catch (DataIntegrityViolationException dup) {
            // Race: another request inserted the same (concertId,email) just now.
            // Treat as idempotent success.
        }
    }
}
