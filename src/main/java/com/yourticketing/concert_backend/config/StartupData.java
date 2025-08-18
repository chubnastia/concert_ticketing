package com.yourticketing.concert_backend.config;

import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.repository.ConcertRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class StartupData {

    @Bean
    CommandLineRunner seedConcerts(ConcertRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                Concert a = new Concert();
                a.setName("Indie Night");
                a.setStartTime(LocalDateTime.now().plusDays(7));
                a.setVenue("Hauptstadt Halle");
                a.setCapacity(100);
                a.setPrice(29.99);
                a.setAvailableTickets(100);

                Concert b = new Concert();
                b.setName("Symphonic Rock");
                b.setStartTime(LocalDateTime.now().plusDays(14));
                b.setVenue("Riverside Arena");
                b.setCapacity(200);
                b.setPrice(49.99);
                b.setAvailableTickets(200);

                repo.saveAll(List.of(a, b));
            }
        };
    }
}
