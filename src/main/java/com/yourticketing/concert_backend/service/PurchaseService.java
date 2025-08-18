package com.yourticketing.concert_backend.service;

import com.yourticketing.concert_backend.dto.BuyDtos.BuyResponse;
import com.yourticketing.concert_backend.model.Reservation;
import com.yourticketing.concert_backend.model.Sale;
import com.yourticketing.concert_backend.repository.ReservationRepository;
import com.yourticketing.concert_backend.repository.SaleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.yourticketing.concert_backend.logging.DomainLog.purchaseCompleted;

@Service
public class PurchaseService {

    private final ReservationRepository reservationRepo;
    private final SaleRepository saleRepo;

    public PurchaseService(ReservationRepository reservationRepo, SaleRepository saleRepo) {
        this.reservationRepo = reservationRepo;
        this.saleRepo = saleRepo;
    }

    @Transactional
    public BuyResponse buy(long reservationId, boolean fail) {
        Reservation r = reservationRepo.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("reservation not found"));

        if (!"ACTIVE".equals(r.getStatus())) {
            throw new IllegalStateException("reservation not active");
        }
        if (r.getExpiry() != null && r.getExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("reservation expired");
        }
        if (fail) {
            throw new IllegalStateException("payment failed (simulated)");
        }

        var existing = saleRepo.findByReservationId(reservationId);
        if (existing.isPresent()) {
            return new BuyResponse(existing.get().getId());
        }

        Sale s = new Sale();
        s.setReservationId(reservationId);
        s.setStatus("COMPLETED");
        s = saleRepo.save(s);

        r.setStatus("BOUGHT");
        reservationRepo.save(r);

        // DOMAIN LOG: purchase completed
        purchaseCompleted(r.getId(), s.getId());

        return new BuyResponse(s.getId());
    }
}
