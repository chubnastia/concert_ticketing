package com.yourticketing.concert_backend.jobs;

import com.yourticketing.concert_backend.service.ExpiryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpirySweepJob {
    private static final Logger log = LoggerFactory.getLogger(ExpirySweepJob.class);
    private final ExpiryService expiryService;

    public ExpirySweepJob(ExpiryService expiryService) {
        this.expiryService = expiryService;
    }

    // Every 5 seconds; configurable via app.expirySweepMs
    @Scheduled(fixedDelayString = "${app.expirySweepMs:5000}")
    public void run() {
        try {
            int expired = expiryService.sweep();
            if (expired > 0) {
                log.info("expired_reservations_sweep count={}", expired);
            }
        } catch (Exception e) {
            log.warn("expiry_sweep_failed msg={}", e.getMessage());
        }
    }
}
