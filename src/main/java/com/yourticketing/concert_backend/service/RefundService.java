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
    private final RestockService restockService;

    public RefundService(SaleRepository saleRepo,
                         ReservationRepository reservationRepo,
                         ConcertRepository concertRepo,
                         RestockService restockService) {
        this.saleRepo = saleRepo;
        this.reservationRepo = reservationRepo;
        this.concertRepo = concertRepo;
        this.restockService = restockService;
    }

    /**
     * Idempotent refund:
     *  - marks sale REFUNDED if not already
     *  - returns tickets to availability once
     *  - sets reservation status to CANCELLED
     *  - triggers watchlist restock notification if we moved 0 -> >0
     */
    @Transactional
    public void cancelPurchase(long saleId) {
        Sale s = saleRepo.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("sale not found"));

        // already refunded? do nothing
        if ("REFUNDED".equals(s.getStatus())) {
            return;
        }

        // mark refunded
        s.setStatus("REFUNDED");
        saleRepo.save(s);

        // fetch the reservation linked to this sale
        Reservation r = reservationRepo.findById(s.getReservationId())
                .orElseThrow(() -> new IllegalStateException("reservation for sale not found"));

        // return tickets to availability (once)
        concertRepo.increment(r.getConcertId(), r.getQuantity());

        // reflect reservation state
        r.setStatus("CANCELLED");
        reservationRepo.save(r);

        // get current availability for logging
        int availableAfter = concertRepo.findById(r.getConcertId())
                .orElseThrow().getAvailableTickets();

        // domain log
        purchaseCancelled(r.getId(), s.getId(), r.getQuantity(), availableAfter);

        // if we flipped from 0 -> >0, this will bump token + enqueue notifications
        restockService.maybeNotifyRestock(r.getConcertId());
    }
}
