package com.yourticketing.concert_backend.repository;

import com.yourticketing.concert_backend.model.Concert;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    @Modifying @Transactional
    @Query("""
        UPDATE Concert c
           SET c.availableTickets = c.availableTickets - :qty
         WHERE c.id = :concertId AND c.availableTickets >= :qty
    """)
    int tryDecrement(@Param("concertId") Long concertId, @Param("qty") int qty);

    @Modifying @Transactional
    @Query("""
        UPDATE Concert c
           SET c.availableTickets = c.availableTickets + :qty
         WHERE c.id = :concertId
    """)
    int increment(@Param("concertId") Long concertId, @Param("qty") int qty);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Concert c where c.id = :id")
    Optional<Concert> findByIdForUpdate(@Param("id") Long id);
}
