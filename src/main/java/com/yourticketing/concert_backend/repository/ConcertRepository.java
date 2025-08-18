package com.yourticketing.concert_backend.repository;

import com.yourticketing.concert_backend.model.Concert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    @Modifying
    @Transactional
    @Query("""
        UPDATE Concert c
           SET c.availableTickets = c.availableTickets - :qty
         WHERE c.id = :concertId AND c.availableTickets >= :qty
    """)
    int tryDecrement(@Param("concertId") Long concertId, @Param("qty") int qty);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Concert c
           SET c.availableTickets = c.availableTickets + :qty
         WHERE c.id = :concertId
    """)
    int increment(@Param("concertId") Long concertId, @Param("qty") int qty);
}
