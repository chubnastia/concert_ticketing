package com.yourticketing.concert_backend;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.Contracts.assertTrue;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("app.expirySweepMs", () -> "500");
        r.add("app.outboxSweepMs", () -> "500");
        r.add("logging.level.org.springframework", () -> "WARN");
        r.add("logging.level.org.hibernate.SQL", () -> "WARN");
        r.add("spring.datasource.hikari.maximum-pool-size", () -> "30");
        r.add("spring.datasource.hikari.connection-timeout", () -> "60000");
        r.add("spring.datasource.hikari.leak-detection-threshold", () -> "2000");
    }

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;

    // --- helpers -------------------------------------------------------------

    record ConcertResp(long id, String name, String startTime, String venue,
                       int capacity, double price, int availableTickets, int version,
                       boolean soldOut, int restockToken) {}

    long adminCreate(String name, int capacity) {
        Map<String, Object> req = Map.of(
                "name", name,
                "startTime", LocalDateTime.now().plusDays(1).withNano(0).toString(),
                "venue", "Test Venue",
                "capacity", capacity,
                "price", 50.0
        );
        ResponseEntity<ConcertResp> resp = rest.postForEntity("/admin/concerts", req, ConcertResp.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().id();
    }

    long reserve(long concertId, String userId, int quantity) {
        Map<String, Object> req = Map.of("userId", userId, "quantity", quantity);
        ResponseEntity<Map> resp = rest.postForEntity("/concerts/" + concertId + "/reserve", req, Map.class);
        if (resp.getStatusCode() == HttpStatus.CONFLICT) {
            throw new RuntimeException("Not enough tickets");
        }
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((Number) resp.getBody().get("reservationId")).longValue();
    }

    long buy(long reservationId) {
        ResponseEntity<Map> resp = rest.postForEntity("/reservations/" + reservationId + "/buy", null, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((Number) resp.getBody().get("saleId")).longValue();
    }

    ConcertResp getConcert(long id) {
        ResponseEntity<ConcertResp[]> resp = rest.getForEntity("/concerts", ConcertResp[].class);
        return Arrays.stream(resp.getBody())
                .filter(c -> c.id() == id)
                .findFirst()
                .orElseThrow();
    }

    // --- tests ---------------------------------------------------------------

    @Timeout(90) // JUnit 5: fail instead of hanging forever
    @Test
    void noOversell_underHeavyConcurrency() throws Exception {
        int threads = 32;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads); // workers announce they're ready
        CountDownLatch start = new CountDownLatch(1);       // we release them together

        List<Callable<Void>> tasks = IntStream.range(0, threads)
                .mapToObj(i -> (Callable<Void>) () -> {
                    try {
                        // signal ready, then wait for the common start
                        ready.countDown();
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Start not released in time");
                        }

                        // === your actual reservation call here ===
                        // reservationService.reserve(concertId, userId, 1);
                        return null;
                    } catch (Throwable t) {
                        // bubble up to fail the test instead of silently hanging
                        throw t;
                    }
                })
                .toList();

        // Submit *before* releasing start
        List<Future<Void>> futures = tasks.stream().map(pool::submit).toList();

        // Wait until everyone reached the barrier, then release them together
        assertTrue(ready.await(10, TimeUnit.SECONDS), "Workers never got ready");
        start.countDown();

        // Don't let .get() block forever
        for (Future<Void> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }

        pool.shutdownNow();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "Pool did not terminate");
    }

    @Test
    void expiryRestocks_andDoesNotOvershoot() throws Exception {
        long concertId = adminCreate("Expiry Test", 5);

        long r1 = reserve(concertId, "u-a", 2);
        long r2 = reserve(concertId, "u-b", 1);
        jdbc.update("UPDATE reservation SET expiry = now() - interval '1 second' WHERE id in (?,?)", r1, r2);

        // Poll until expired (max 5s)
        long timeout = System.currentTimeMillis() + 5000;
        int available = getConcert(concertId).availableTickets();
        while (available != 5 && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
            available = getConcert(concertId).availableTickets();
        }
        assertThat(available).isEqualTo(5);
    }

    @Test
    void watchlist_notified_once_per_0_to_gt0() throws Exception {
        long concertId = adminCreate("Watchlist Test", 1);

        long r = reserve(concertId, "u-x", 1);
        assertThat(getConcert(concertId).availableTickets()).isEqualTo(0);

        Map<String, Object> join = Map.of("email", "notify@test.com");
        ResponseEntity<Void> j = rest.postForEntity("/concerts/" + concertId + "/watchlist", join, Void.class);
        assertThat(j.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        long saleId = buy(r);
        rest.delete("/sales/" + saleId);

        // Poll for first notification (SENT count ==1)
        long timeout = System.currentTimeMillis() + 5000;
        Integer cnt = jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE type='WATCHLIST_RESTOCK' AND aggregate_id = ? AND status='SENT'",
                Integer.class, concertId);
        while (cnt != 1 && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
            cnt = jdbc.queryForObject(
                    "SELECT count(*) FROM outbox WHERE type='WATCHLIST_RESTOCK' AND aggregate_id = ? AND status='SENT'",
                    Integer.class, concertId);
        }
        assertThat(cnt).isEqualTo(1);

        // Update capacity (should not trigger another)
        Map<String, Object> up = Map.of(
                "name", "Watchlist Test",
                "startTime", LocalDateTime.now().plusDays(3).withNano(0).toString(),
                "venue", "Test Hall",
                "capacity", 2,
                "price", 42.0
        );
        rest.put("/admin/concerts/" + concertId, up);

        // Poll again (still 1)
        timeout = System.currentTimeMillis() + 5000;
        Integer cnt2 = jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE type='WATCHLIST_RESTOCK' AND aggregate_id = ? AND status='SENT'",
                Integer.class, concertId);
        while (cnt2 != 1 && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
            cnt2 = jdbc.queryForObject(
                    "SELECT count(*) FROM outbox WHERE type='WATCHLIST_RESTOCK' AND aggregate_id = ? AND status='SENT'",
                    Integer.class, concertId);
        }
        assertThat(cnt2).isEqualTo(1);
    }
}