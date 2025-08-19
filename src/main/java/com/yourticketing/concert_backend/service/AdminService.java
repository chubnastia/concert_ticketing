package com.yourticketing.concert_backend.service;

import com.yourticketing.concert_backend.dto.AdminConcertDtos.CreateRequest;
import com.yourticketing.concert_backend.dto.AdminConcertDtos.UpdateRequest;
import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final ConcertRepository concertRepo;
    private final RestockService restockService;

    public AdminService(ConcertRepository concertRepo, RestockService restockService) {
        this.concertRepo = concertRepo;
        this.restockService = restockService;
    }

    @Transactional
    public Concert create(CreateRequest req) {
        Concert c = new Concert();
        c.setName(req.name);
        c.setStartTime(req.startTime);
        c.setVenue(req.venue);
        c.setCapacity(req.capacity);
        c.setPrice(req.price);
        c.setAvailableTickets(req.capacity);
        c.setSoldOut(req.capacity == 0);
        c.setRestockToken(0);
        return concertRepo.save(c);
    }

    /**
     * Full update (PUT semantics). Capacity math:
     * - Let committed = oldCapacity - availableTickets (already promised seats: sold+reserved).
     * - newCapacity must be >= committed, else 409 (cannot overbook).
     * - delta = newCapacity - oldCapacity
     *   - if delta > 0: availableTickets += delta
     *   - if delta < 0: availableTickets -= min(availableTickets, -delta) (we validated so it's exact)
     * Then we let RestockService adjust soldOut flag and maybe enqueue notifications for 0 -> >0.
     */
    @Transactional
    public Concert update(long id, UpdateRequest req) {
        Concert c = concertRepo.findByIdForUpdate(id).orElseThrow();

        int oldCap = c.getCapacity();
        int committed = oldCap - c.getAvailableTickets();
        if (req.capacity < committed) {
            throw new IllegalStateException(
                    "cannot reduce capacity below committed seats (" + committed + ")"
            );
        }

        // Apply non-capacity fields
        c.setName(req.name);
        c.setStartTime(req.startTime);
        c.setVenue(req.venue);
        c.setPrice(req.price);

        // Apply capacity delta
        int delta = req.capacity - oldCap;
        if (delta != 0) {
            if (delta > 0) {
                c.setAvailableTickets(c.getAvailableTickets() + delta);
            } else {
                int reduce = Math.min(c.getAvailableTickets(), -delta);
                c.setAvailableTickets(c.getAvailableTickets() - reduce);
            }
            c.setCapacity(req.capacity);
        } else {
            c.setCapacity(req.capacity);
        }

        Concert saved = concertRepo.save(c);

        // Handle potential sold-out transitions and restock notifications
        restockService.maybeNotifyRestock(saved.getId());

        return saved;
    }
}
