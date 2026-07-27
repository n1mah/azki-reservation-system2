package com.azki.reservation.mapper;

import com.azki.reservation.dto.ReservationResponse;
import com.azki.reservation.service.ReservationService.ReservationResult;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationResponse toResponse(ReservationResult result) {
        return new ReservationResponse(
                result.reservation().getId(),
                result.slot().getId(),
                result.slot().getStartTime(),
                result.slot().getEndTime(),
                result.reservation().getStatus().name());
    }
}