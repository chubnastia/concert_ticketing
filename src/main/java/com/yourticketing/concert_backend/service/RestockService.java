package com.yourticketing.concert_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.model.Outbox;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import com.yourticketing.concert_backend.repository.OutboxRepository;
import com.yourticketing.concert_backend.repository.WatchlistRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RestockService {

    private final ConcertRepository concertRepo;
    private final WatchlistRepository watchlistRepo;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper om = new ObjectMapper();

    public RestockService(ConcertRepository concertRepo,
                          WatchlistRepository watchlistRepo,
                          OutboxRepository outboxRepo) {
        this.concertRepo = concertRepo;
        this.watchlistRepo = watchlistRepo;
        this.outboxRepo = outboxRepo;
    }

    /** Call after any operation that may increase availability (refund, expiry, capacity increase). */
    @Transactional
    public void maybeNotifyRestock(Long concertId) {
        Concert c = concertRepo.findByIdForUpdate(concertId).orElseThrow();
        boolean wasSoldOut = c.isSoldOut();
        boolean nowSoldOut = (c.getAvailableTickets() == 0);

        if (wasSoldOut && !nowSoldOut) {
            // 0 -> >0 transition: flip flag and bump token
            c.setSoldOut(false);
            c.setRestockToken(c.getRestockToken() + 1);
            concertRepo.save(c);

            int token = c.getRestockToken();
            // ensure one outbox per (concert, token)
            if (outboxRepo.findByTypeAndAggregateIdAndToken("WATCHLIST_RESTOCK", concertId, token).isEmpty()) {
                Outbox ob = new Outbox();
                ob.setType("WATCHLIST_RESTOCK");
                ob.setAggregateId(concertId);
                ob.setToken(token);

                Map<String, Object> payload = new HashMap<>();
                payload.put("concertId", concertId);
                payload.put("token", token);
                payload.put("watchlistCount", watchlistRepo.findByConcertId(concertId).size());

                try {
                    ob.setPayload(om.writeValueAsString(payload));
                } catch (Exception e) {
                    ob.setPayload("{\"concertId\":" + concertId + ",\"token\":" + token + "}");
                }

                outboxRepo.save(ob);
            }
        } else if (!wasSoldOut && nowSoldOut) {
            // >0 -> 0 transition: flip flag
            c.setSoldOut(true);
            concertRepo.save(c);
        }
    }

    /** Call after a successful reserve when availability *might* have reached 0. */
    @Transactional
    public void updateSoldOutIfZero(Long concertId) {
        Concert c = concertRepo.findByIdForUpdate(concertId).orElseThrow();
        if (c.getAvailableTickets() == 0 && !c.isSoldOut()) {
            c.setSoldOut(true);
            concertRepo.save(c);
        }
    }
}
