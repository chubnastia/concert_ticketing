package com.yourticketing.concert_backend.repository;

import com.yourticketing.concert_backend.model.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertRepository extends JpaRepository<Concert, Long> {
}