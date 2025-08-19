package com.yourticketing.concert_backend;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
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
        // faster background jobs during tests
        r.add("app.expirySweepMs", () -> "500");
        r.add("app.outboxSweepMs", () -> "500");
        r.add("logging.level.org.springframework", () -> "WARN");
        r.add("logging.level.org.hibernate.SQL", () -> "WARN");
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
                "startTime", LocalDateTime.now().plusDays(3).withNano(0).toString(),
                "venue", "Test Hall",
                "capacity", capacity,
                "price", 42.0
        );
        ResponseEntity<ConcertResp> resp = rest.postForEntity("/admin/concerts", req, ConcertResp.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return Objects.requireNonNull(resp.getBody()).id();
    }

    long reserve(long concertId, String userId, int qty) {
        Map<String, Object> body = Map.of("userId", userId, "quantity", qty);
        var resp = rest.postForEntity("/concerts/" + concertId + "/reserve", body, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Object id = Objects.requireNonNull(resp.getBody()).get("reservationId");
        return ((Number) id).longValue();
    }

    long buy(long reservationId) {
        var resp = rest.postForEntity("/reservations/" + reservationId + "/buy", null, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Object id = Objects.requireNonNull(resp.getBody()).get("saleId");
        return ((Number) id).longValue();
    }

    ConcertResp getConcert(long id) {
        var list = rest.getForEntity("/concerts", ConcertResp[].class).getBody();
        assertThat(list).isNotNull();
        return Arrays.stream(list).filter(c -> c.id()==id).findFirst().orElseThrow();
    }

    // --- tests ---------------------------------------------------------------

    @Test
    void noOversell_underHeavyConcurrency() throws Exception {
        long concertId = adminCreate("Oversell Test", 50);

        int attempts = 200;
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            final int n = i;
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    Map<String, Object> body = Map.of("userId", "u-" + n, "quantity", 1);
                    ResponseEntity<Map> resp = rest.postForEntity("/concerts/" + concertId + "/reserve", body, Map.class);
                    if (resp.getStatusCode().is2xxSuccessful()) successes.incrementAndGet();
                } catch (Exception ignored) {}
            }));
        }

        start.countDown();
        for (Future<?> f : futures) f.get(15, TimeUnit.SECONDS);
        pool.shutdown();

        // Verify exactly 50 succeeded and availability is 0
        assertThat(successes.get()).isEqualTo(50);
        assertThat(getConcert(concertId).availableTickets()).isEqualTo(0);
    }

    @Test
    void expiryRestocks_andDoesNotOvershoot() throws Exception {
        long concertId = adminCreate("Expiry Test", 5);

        long r1 = reserve(concertId, "u-a", 2);
        long r2 = reserve(concertId, "u-b", 1);
        // Make both reservations expire immediately
        jdbc.update("UPDATE reservation SET expiry = now() - interval '1 second' WHERE id in (?,?)", r1, r2);

        // Wait a bit for the sweep to process
        Thread.sleep(1500);

        // After expiry: availableTickets back to 5
        assertThat(getConcert(concertId).availableTickets()).isEqualTo(5);
    }

    @Test
    void watchlist_notified_once_per_0_to_gt0() throws Exception {
        long concertId = adminCreate("Watchlist Test", 1);

        // Sell out: reserve 1
        long r = reserve(concertId, "u-x", 1);
        assertThat(getConcert(concertId).availableTickets()).isEqualTo(0);

        // Join watchlist while sold out
        Map<String, Object> join = Map.of("email", "notify@test.com");
        ResponseEntity<Void> j = rest.postForEntity("/concerts/" + concertId + "/watchlist", join, Void.class);
        assertThat(j.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // Refund to make 0 -> >0
        long saleId = buy(r);
        rest.delete("/sales/" + saleId);

        // Wait for outbox dispatcher
        Thread.sleep(1500);

        Integer cnt = jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE type='WATCHLIST_RESTOCK' AND aggregate_id = ? AND status='SENT'",
                Integer.class, concertId);
        assertThat(cnt).isEqualTo(1);

        // Increase capacity again while already >0: should NOT produce another row
        Map<String, Object> up = Map.of(
                "name","Watchlist Test",
                "startTime", LocalDateTime.now().plusDays(3).withNano(0).toString(),
                "venue","Test Hall",
                "capacity", 2,
                "price", 42.0
        );
        rest.put("/admin/concerts/" + concertId, up);

        Thread.sleep(1000);

        Integer cnt2 = jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE type='WATCHLIST_RESTOCK' AND aggregate_id = ? AND status='SENT'",
                Integer.class, concertId);
        assertThat(cnt2).isEqualTo(1); // still one
    }
}
