package com.azki.reservation.dto;

import java.time.LocalDateTime;

public class ReservationRequest {

    private LocalDateTime earliestFrom;

    public LocalDateTime getEarliestFrom() { return earliestFrom; }
    public void setEarliestFrom(LocalDateTime earliestFrom) { this.earliestFrom = earliestFrom; }
}