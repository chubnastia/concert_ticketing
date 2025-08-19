package com.yourticketing.concert_backend.service;

import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestockService {

    private final ConcertRepository concertRepo;
    private final JdbcTemplate jdbc;

    public RestockService(ConcertRepository concertRepo, JdbcTemplate jdbc) {
        this.concertRepo = concertRepo;
        this.jdbc = jdbc;
    }

    /**
     * If a concert has zero available tickets, mark it soldOut=true;
     * otherwise soldOut=false. Safe to call after reserve/cancel/expiry.
     */
    @Transactional
    public void updateSoldOutIfZero(Long concertId) {
        Concert c = concertRepo.findById(concertId).orElseThrow();
        boolean shouldBeSoldOut = c.getAvailableTickets() <= 0;
        if (c.isSoldOut() != shouldBeSoldOut) {
            c.setSoldOut(shouldBeSoldOut);
            concertRepo.save(c);
        }
    }

    /**
     * When availability becomes > 0, bump restockToken and enqueue a WATCHLIST_RESTOCK outbox item.
     * Idempotency is handled by the token—dispatchers should only notify once per (0 -> >0) transition.
     */
    @Transactional
    public void maybeNotifyRestock(long concertId) {
        Concert c = concertRepo.findById(concertId).orElseThrow();

        // Only notify on >0 availability
        if (c.getAvailableTickets() <= 0) {
            return;
        }

        long newToken = c.getRestockToken() + 1L;
        c.setRestockToken(newToken);
        c.setSoldOut(false);
        concertRepo.save(c);

        jdbc.update(
                "INSERT INTO outbox(type, aggregate_id, token, status, created_at) VALUES (?, ?, ?, ?, now())",
                "WATCHLIST_RESTOCK", concertId, newToken, "PENDING"
        );
    }
}
