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

    public ExpiryService(ReservationRepository reservationRepo, ConcertRepository concertRepo) {
        this.reservationRepo = reservationRepo;
        this.concertRepo = concertRepo;
    }

    /** Expires all overdue ACTIVE reservations. Returns number expired. */
    @Transactional
    public int sweep() {
        List<Reservation> due = reservationRepo.findAllActiveExpired(LocalDateTime.now());
        int count = 0;
        for (Reservation r0 : due) {
            // Lock per row to be safe across instances
            reservationRepo.findByIdForUpdate(r0.getId()).ifPresent(r -> {
                if (!"ACTIVE".equals(r.getStatus())) return;
                if (r.getExpiry().isAfter(LocalDateTime.now())) return; // re-check
                r.setStatus("EXPIRED");
                reservationRepo.save(r);
                concertRepo.increment(r.getConcertId(), r.getQuantity());
                reservationExpired(r.getConcertId(), r.getId(), r.getQuantity());
            });
            count++;
        }
        return count;
    }
}
