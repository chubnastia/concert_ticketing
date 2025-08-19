package com.yourticketing.concert_backend.service;

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
    public Concert update(long id, UpdateRequest req) {
        Concert c = concertRepo.findById(id).orElseThrow();

        int beforeAvailable = c.getAvailableTickets();
        int beforeCapacity  = c.getCapacity();

        c.setName(req.name());
        c.setStartTime(req.startTime());
        c.setVenue(req.venue());
        c.setCapacity(req.capacity());
        c.setPrice(req.price());

        // Adjust availableTickets when capacity changes
        int deltaCap = req.capacity() - beforeCapacity;
        int newAvail = beforeAvailable + deltaCap;

        if (newAvail < 0) newAvail = 0;
        if (newAvail > req.capacity()) newAvail = req.capacity();

        c.setAvailableTickets(newAvail);
        c.setSoldOut(newAvail == 0);

        Concert saved = concertRepo.save(c);

        // If we transition from 0 -> >0, notify watchlist
        if (beforeAvailable == 0 && saved.getAvailableTickets() > 0) {
            restockService.maybeNotifyRestock(saved.getId());
        }
        return saved;
    }
}
