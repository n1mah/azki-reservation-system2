package com.azki.reservation.dto;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long reservationId,
        Long slotId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status) {
}