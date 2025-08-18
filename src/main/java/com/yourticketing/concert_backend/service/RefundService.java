package com.yourticketing.concert_backend.service;

import com.yourticketing.concert_backend.model.Reservation;
import com.yourticketing.concert_backend.model.Sale;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import com.yourticketing.concert_backend.repository.ReservationRepository;
import com.yourticketing.concert_backend.repository.SaleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import static com.yourticketing.concert_backend.logging.DomainLog.purchaseCancelled;

@Service
public class RefundService {

    private final SaleRepository saleRepo;
    private final ReservationRepository reservationRepo;
    private final ConcertRepository concertRepo;

    public RefundService(SaleRepository saleRepo,
                         ReservationRepository reservationRepo,
                         ConcertRepository concertRepo) {
        this.saleRepo = saleRepo;
        this.reservationRepo = reservationRepo;
        this.concertRepo = concertRepo;
    }

    @Transactional
    public void cancelPurchase(long saleId) {
        Sale s = saleRepo.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("sale not found"));

        if ("REFUNDED".equals(s.getStatus())) {
            return; // idempotent
        }

        s.setStatus("REFUNDED");
        saleRepo.save(s);

        Reservation r = reservationRepo.findById(s.getReservationId())
                .orElseThrow(() -> new IllegalStateException("reservation for sale not found"));

        // Return tickets to availability
        concertRepo.increment(r.getConcertId(), r.getQuantity());

        // Reflect reservation state
        r.setStatus("CANCELLED");
        reservationRepo.save(r);

        // DOMAIN LOG: purchase cancelled / refunded
        // If you want the exact 'available_after', you can reload the concert here.
        purchaseCancelled(r.getId(), s.getId(), r.getQuantity(), -1);
    }
}
