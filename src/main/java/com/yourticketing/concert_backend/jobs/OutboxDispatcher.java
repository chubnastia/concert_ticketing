package com.yourticketing.concert_backend.jobs;

import com.yourticketing.concert_backend.model.Outbox;
import com.yourticketing.concert_backend.model.Watchlist;
import com.yourticketing.concert_backend.repository.OutboxRepository;
import com.yourticketing.concert_backend.repository.WatchlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger("domain");

    private final OutboxRepository outboxRepo;
    private final WatchlistRepository watchlistRepo;

    public OutboxDispatcher(OutboxRepository outboxRepo, WatchlistRepository watchlistRepo) {
        this.outboxRepo = outboxRepo;
        this.watchlistRepo = watchlistRepo;
    }

    @Scheduled(fixedDelayString = "${app.outboxSweepMs:5000}")
    @Transactional
    public void run() {
        List<Outbox> batch = outboxRepo.findTop100ByStatusOrderByIdAsc("PENDING");
        for (Outbox ob : batch) {
            if ("WATCHLIST_RESTOCK".equals(ob.getType())) {
                List<Watchlist> wl = watchlistRepo.findByConcertId(ob.getAggregateId());
                log.info("watchlist_notified concert_id={} token={} count={}",
                        ob.getAggregateId(), ob.getToken(), wl.size());
            }
            ob.setStatus("SENT");
            outboxRepo.save(ob);
        }
    }
}
