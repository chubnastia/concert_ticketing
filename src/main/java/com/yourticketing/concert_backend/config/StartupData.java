package com.yourticketing.concert_backend.config;

import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class StartupData {

    @Bean
    public org.springframework.boot.CommandLineRunner seed(ConcertRepository concerts) {
        return args -> {
            if (concerts.count() == 0) {
                Concert a = new Concert();
                a.setName("Indie Night");
                a.setStartTime(LocalDateTime.now().plusDays(7));
                a.setVenue("Hauptstadt Halle");
                a.setCapacity(100);
                a.setAvailableTickets(100);
                a.setPrice(BigDecimal.valueOf(29.99));
                a.setSoldOut(false);
                a.setRestockToken(0L);
                concerts.save(a);

                Concert b = new Concert();
                b.setName("Symphonic Rock");
                b.setStartTime(LocalDateTime.now().plusDays(14));
                b.setVenue("Riverside Arena");
                b.setCapacity(200);
                b.setAvailableTickets(200);
                b.setPrice(BigDecimal.valueOf(49.99));
                b.setSoldOut(false);
                b.setRestockToken(0L);
                concerts.save(b);
            }
        };
    }
}
