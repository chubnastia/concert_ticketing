package com.yourticketing.concert_backend.service;

import com.yourticketing.concert_backend.dto.ReserveDtos.ReserveRequest;
import com.yourticketing.concert_backend.dto.ReserveDtos.ReserveResponse;
import com.yourticketing.concert_backend.model.Reservation;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import com.yourticketing.concert_backend.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.yourticketing.concert_backend.logging.DomainLog.reservationPlaced;

@Service
public class ReservationService {

    private final ConcertRepository concertRepo;
    private final ReservationRepository resRepo;

    private final RestockService restockService;

    public ReservationService(ConcertRepository concertRepo, ReservationRepository resRepo, RestockService restockService) {
        this.concertRepo = concertRepo;
        this.resRepo = resRepo;
        this.restockService = restockService;
    }

    @Transactional
    public ReserveResponse reserve(ReserveRequest req) {
        if (req.quantity < 1 || req.quantity > 6) {
            throw new IllegalArgumentException("quantity must be 1..6");
        }
        if (req.concertId == null) {
            throw new IllegalArgumentException("concertId is required");
        }

        int updated = concertRepo.tryDecrement(req.concertId, req.quantity);
        if (updated == 0) {
            throw new IllegalStateException("Not enough tickets available");
        }

        Reservation r = new Reservation();
        r.setConcertId(req.concertId);
        r.setUserId(req.userId);
        r.setQuantity(req.quantity);
        r.setExpiry(LocalDateTime.now().plusSeconds(120));
        r.setStatus("ACTIVE");
        r = resRepo.save(r);

        // DOMAIN LOG: reservation placed (held)
        reservationPlaced(req.concertId, r.getId(), req.quantity);
        restockService.updateSoldOutIfZero(req.concertId);

        return new ReserveResponse(r.getId());
    }
}
