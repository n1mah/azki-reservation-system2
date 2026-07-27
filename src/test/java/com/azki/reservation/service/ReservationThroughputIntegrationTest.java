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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest
class ReservationThroughputIntegrationTest {

    private static final int CONCURRENT_USERS = 50;

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0")
            .withDatabaseName("azki_reservation_throughput")
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

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        slotRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("When enough slots exist, every concurrent user receives a distinct slot")
    void everyUserReceivesDistinctSlot() throws InterruptedException {
        createSlots(CONCURRENT_USERS);
        List<Long> userIds = createUsers(CONCURRENT_USERS);

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch startGate = new CountDownLatch(1);
        Set<Long> reservedSlotIds = ConcurrentHashMap.newKeySet();
        AtomicInteger succeeded = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (Long userId : userIds) {
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                    ReservationService.ReservationResult result =
                            reservationService.reserveNearestSlot(userId, null);
                    reservedSlotIds.add(result.slot().getId());
                    succeeded.incrementAndGet();
                } catch (NoAvailableSlotException e) {
                    Thread.currentThread().interrupt();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        startGate.countDown();
        awaitCompletion(futures);
        pool.shutdown();

        assertEquals(CONCURRENT_USERS, succeeded.get(), "every user must receive a slot");
        assertEquals(CONCURRENT_USERS, reservedSlotIds.size(), "no slot may be handed out twice");
        assertEquals(CONCURRENT_USERS, reservationRepository.count());
    }

    private List<Long> createSlots(int count) {
        LocalDateTime base = LocalDateTime.now().plusHours(1);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AvailableSlot slot = new AvailableSlot();
            slot.setStartTime(base.plusHours(i));
            slot.setEndTime(base.plusHours(i + 1));
            slot.setReserved(false);
            ids.add(slotRepository.save(slot).getId());
        }
        return ids;
    }

    private List<Long> createUsers(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setUsername("throughput_user_" + i);
            user.setEmail("throughput_" + i + "@test.local");
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