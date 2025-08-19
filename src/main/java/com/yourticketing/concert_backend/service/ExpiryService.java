package com.yourticketing.concert_backend.service;

import com.yourticketing.concert_backend.model.Reservation;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import com.yourticketing.concert_backend.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.yourticketing.concert_backend.logging.DomainLog.reservationExpired;

@Service
public class ExpiryService {

    private final ReservationRepository reservationRepo;
    private final ConcertRepository concertRepo;
    private final RestockService restockService;

    public ExpiryService(ReservationRepository reservationRepo,
                         ConcertRepository concertRepo,
                         RestockService restockService) {
        this.reservationRepo = reservationRepo;
        this.concertRepo = concertRepo;
        this.restockService = restockService;
    }

    /** Expires all overdue ACTIVE reservations. Returns number that actually transitioned to EXPIRED. */
    @Transactional
    public int sweep() {
        final LocalDateTime now = LocalDateTime.now();
        List<Reservation> candidates = reservationRepo.findAllActiveExpired(now);

        int expiredCount = 0;

        for (Reservation probe : candidates) {
            final boolean[] transitioned = { false };

            // Lock the row to be safe across instances
            reservationRepo.findByIdForUpdate(probe.getId()).ifPresent(r -> {
                if (!"ACTIVE".equals(r.getStatus())) return;
                if (r.getExpiry() != null && r.getExpiry().isAfter(now)) return; // re-check with captured 'now'

                // Mark expired and return tickets exactly once
                r.setStatus("EXPIRED");
                reservationRepo.save(r);

                concertRepo.increment(r.getConcertId(), r.getQuantity());
                restockService.maybeNotifyRestock(r.getConcertId());

                reservationExpired(r.getConcertId(), r.getId(), r.getQuantity());
                transitioned[0] = true;
            });

            if (transitioned[0]) {
                expiredCount++;
            }
        }

        return expiredCount;
    }
}
