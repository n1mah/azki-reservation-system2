package com.azki.reservation.repository;

import com.azki.reservation.entity.AvailableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AvailableSlotRepository extends JpaRepository<AvailableSlot, Long> {

    @Query(value = """
            SELECT * FROM available_slots
            WHERE is_reserved = false
              AND start_time >= :from
            ORDER BY start_time ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<AvailableSlot> findAndLockNearestAvailableSlot(@Param("from") LocalDateTime from);

    List<AvailableSlot> findTop20ByReservedFalseAndStartTimeGreaterThanEqualOrderByStartTimeAsc(LocalDateTime from);
}