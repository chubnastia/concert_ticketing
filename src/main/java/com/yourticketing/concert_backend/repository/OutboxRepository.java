package com.yourticketing.concert_backend.repository;

import com.yourticketing.concert_backend.model.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findTop100ByStatusOrderByIdAsc(String status);
    Optional<Outbox> findByTypeAndAggregateIdAndToken(String type, Long aggregateId, int token);
}
