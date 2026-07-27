package com.azki.reservation.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record AvailableSlotResponse(
        Long id,
        LocalDateTime startTime,
        LocalDateTime endTime) implements Serializable {
}