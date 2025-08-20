package com.yourticketing.concert_backend.service;

import com.yourticketing.concert_backend.dto.AdminConcertDtos;
import com.yourticketing.concert_backend.dto.AdminConcertDtos.CreateRequest;
import com.yourticketing.concert_backend.dto.AdminConcertDtos.UpdateRequest;
import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
        c.setName(req.name());
        c.setStartTime(req.startTime());
        c.setVenue(req.venue());
        c.setCapacity(req.capacity());
        c.setPrice(req.price() != null ? req.price() : BigDecimal.ZERO);
        c.setAvailableTickets(req.capacity());
        c.setSoldOut(req.capacity() <= 0);
        c.setRestockToken(0L);
        return concertRepo.save(c);
    }

    @Transactional
    public Concert update(long id, AdminConcertDtos.UpdateRequest r) {
        Concert c = concertRepo.findById(id).orElseThrow();

        // copy mutable fields
        c.setName(r.name());
        c.setStartTime(r.startTime());
        c.setVenue(r.venue());
        c.setPrice(r.price());

        // compute capacity delta
        int oldCap = c.getCapacity();
        int newCap = r.capacity();
        int delta  = newCap - oldCap;

        // always set the new capacity
        c.setCapacity(newCap);

        // if capacity increased, add delta to available tickets
        if (delta > 0) {
            c.setAvailableTickets(c.getAvailableTickets() + delta);
        } else if (delta < 0) {
            // clamp so we never go negative on availability
            int newAvail = Math.max(0, c.getAvailableTickets() + delta);
            c.setAvailableTickets(newAvail);
        }

        boolean wasSoldOut = c.isSoldOut();
        boolean nowSoldOut = (c.getAvailableTickets() == 0);
        c.setSoldOut(nowSoldOut);

        // persist core changes
        concertRepo.save(c);

        // if we transitioned 0 -> >0, bump token and enqueue a single notify
        if (wasSoldOut && !nowSoldOut) {
            int newToken = (int) (c.getRestockToken() + 1);
            c.setRestockToken(newToken);
            concertRepo.save(c); // persist token bump
        }

        return c;
    }
}