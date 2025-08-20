package com.yourticketing.concert_backend.service;

import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.model.Watchlist;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import com.yourticketing.concert_backend.repository.WatchlistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepo;
    private final ConcertRepository concertRepo;

    public WatchlistService(WatchlistRepository watchlistRepo, ConcertRepository concertRepo) {
        this.watchlistRepo = watchlistRepo;
        this.concertRepo = concertRepo;
    }

    @Transactional
    public void join(Long concertId, String email) {
        Concert concert = concertRepo.findById(concertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concert not found"));

        if (!concert.isSoldOut()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Concert is not sold out");
        }

        // Check for duplicate (optional, add unique constraint if needed)
        if (watchlistRepo.existsByConcertIdAndEmail(concertId, email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already on watchlist");
        }

        Watchlist w = new Watchlist();
        w.setConcertId(concertId);
        w.setEmail(email);
        watchlistRepo.save(w);
    }
}