package com.azki.reservation.service;

import com.azki.reservation.entity.AvailableSlot;
import com.azki.reservation.entity.User;
import com.azki.reservation.exception.NoAvailableSlotException;
import com.azki.reservation.repository.AvailableSlotRepository;
import com.azki.reservation.repository.ReservationRepository;
import com.azki.reservation.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
class ReservationConcurrencyIntegrationTest {

    private static final int CONCURRENT_USERS = 50;

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0")
            .withDatabaseName("azki_reservation_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private AvailableSlotRepository slotRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    private Long contestedSlotId;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        slotRepository.deleteAll();
        userRepository.deleteAll();

        AvailableSlot slot = new AvailableSlot();
        slot.setStartTime(LocalDateTime.now().plusHours(1));
        slot.setEndTime(LocalDateTime.now().plusHours(2));
        slot.setReserved(false);
        contestedSlotId = slotRepository.save(slot).getId();
    }

    @Test
    @DisplayName("When many users race for the only free slot, exactly one succeeds")
    void onlyOneUserWinsTheContestedSlot() throws InterruptedException {
        List<Long> userIds = createUsers(CONCURRENT_USERS);

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (Long userId : userIds) {
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                    reservationService.reserveNearestSlot(userId, null);
                    succeeded.incrementAndGet();
                } catch (NoAvailableSlotException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        startGate.countDown();
        awaitCompletion(futures);
        pool.shutdown();

        assertEquals(1, succeeded.get(), "exactly one reservation must succeed");
        assertEquals(CONCURRENT_USERS - 1, rejected.get(), "all other attempts must be rejected");
        assertEquals(1, reservationRepository.count(), "database must hold exactly one reservation");
        assertTrue(slotRepository.findById(contestedSlotId).orElseThrow().isReserved());
    }

    private List<Long> createUsers(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setUsername("racer_" + i);
            user.setEmail("racer_" + i + "@test.local");
            user.setPassword("not-used-in-this-test");
            ids.add(userRepository.save(user).getId());
        }
        return ids;
    }

    private void awaitCompletion(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
    }
}