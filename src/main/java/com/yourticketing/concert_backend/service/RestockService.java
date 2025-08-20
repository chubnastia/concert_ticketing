package com.yourticketing.concert_backend.service;

import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.model.Outbox;
import com.yourticketing.concert_backend.model.Watchlist;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import com.yourticketing.concert_backend.repository.OutboxRepository;
import com.yourticketing.concert_backend.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestockService {

    private final ConcertRepository concertRepo;
    private final WatchlistRepository watchlistRepo;
    private final OutboxRepository outboxRepo;

    public RestockService(ConcertRepository concertRepo, WatchlistRepository watchlistRepo, OutboxRepository outboxRepo) {
        this.concertRepo = concertRepo;
        this.watchlistRepo = watchlistRepo;
        this.outboxRepo = outboxRepo;
    }

    @Transactional
    public void maybeNotifyRestock(Long concertId) {
        Concert concert = concertRepo.findById(concertId).orElseThrow();
        if (concert.getAvailableTickets() <= 0) return;  // No need if still sold out

        // Fetch emails
        List<String> emails = watchlistRepo.findByConcertId(concertId).stream()
                .map(Watchlist::getEmail)
                .collect(Collectors.toList());

        if (emails.isEmpty()) return;

        // Create outbox event (payload as JSON string)
        Outbox out = new Outbox();
        out.setType("WATCHLIST_RESTOCK");
        out.setAggregateId(concertId);
        out.setToken((int) concert.getRestockToken());
        out.setPayload("{\"concertId\":" + concertId + ", \"emails\":[" + emails.stream().map(e -> "\"" + e + "\"").collect(Collectors.joining(",")) + "]}");
        outboxRepo.save(out);

        // Log as "sent" for simulation
        System.out.println("Notification logged for concert " + concertId + ": Emails - " + emails);
    }
    @Transactional
    public void updateSoldOutIfZero(Long concertId) {
        Concert c = concertRepo.findById(concertId).orElseThrow();
        boolean shouldBeSoldOut = c.getAvailableTickets() <= 0;
        if (c.isSoldOut() != shouldBeSoldOut) {
            c.setSoldOut(shouldBeSoldOut);
            concertRepo.save(c);
        }
    }
}