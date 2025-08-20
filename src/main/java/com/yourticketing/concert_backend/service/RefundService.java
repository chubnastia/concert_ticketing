package com.yourticketing.concert_backend.service;

import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.model.Reservation;
import com.yourticketing.concert_backend.model.Sale;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import com.yourticketing.concert_backend.repository.ReservationRepository;
import com.yourticketing.concert_backend.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Return tickets to availability - load, update, save for optimistic lock
        Concert concert = concertRepo.findById(r.getConcertId())
                .orElseThrow(() -> new IllegalStateException("concert not found"));
        boolean wasSoldOut = concert.isSoldOut();
        concert.setAvailableTickets(concert.getAvailableTickets() + r.getQuantity());
        if (concert.getAvailableTickets() > concert.getCapacity()) {
            concert.setAvailableTickets(concert.getCapacity());
        }
        concert.setSoldOut(concert.getAvailableTickets() <= 0);
        concertRepo.save(concert);

        int availableAfter = concert.getAvailableTickets();

        // Reflect reservation state
        r.setStatus("CANCELLED");
        reservationRepo.save(r);

        // DOMAIN LOG: purchase cancelled / refunded
        purchaseCancelled(r.getId(), s.getId(), r.getQuantity(), availableAfter);

        // Notify watchlist if transitioned from sold out
        if (wasSoldOut && !concert.isSoldOut()) {
            restockService.maybeNotifyRestock(r.getConcertId());
        }
    }
}