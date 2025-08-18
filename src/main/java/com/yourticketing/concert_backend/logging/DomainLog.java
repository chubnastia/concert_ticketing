package com.yourticketing.concert_backend.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.logstash.logback.argument.StructuredArguments.kv;

public final class DomainLog {
    private static final Logger log = LoggerFactory.getLogger("domain");
    private DomainLog() {}

    public static void reservationPlaced(long concertId, long reservationId, int qty) {
        log.info("reservation_placed", kv("concert_id", concertId), kv("reservation_id", reservationId), kv("qty", qty));
    }
    public static void reservationExpired(long concertId, long reservationId, int qty) {
        log.info("reservation_expired", kv("concert_id", concertId), kv("reservation_id", reservationId), kv("qty", qty));
    }
    public static void purchaseCompleted(long reservationId, long saleId) {
        log.info("purchase_completed", kv("reservation_id", reservationId), kv("sale_id", saleId));
    }
    public static void purchaseCancelled(long reservationId, long saleId, int qty, int availableAfter) {
        log.info("purchase_cancelled", kv("reservation_id", reservationId), kv("sale_id", saleId),
                kv("qty", qty), kv("available_after", availableAfter));
    }
}
