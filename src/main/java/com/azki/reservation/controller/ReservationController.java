package com.azki.reservation.controller;

import com.azki.reservation.dto.AvailableSlotResponse;
import com.azki.reservation.dto.ReservationRequest;
import com.azki.reservation.dto.ReservationResponse;
import com.azki.reservation.mapper.ReservationMapper;
import com.azki.reservation.security.AuthenticatedUser;
import com.azki.reservation.service.ReservationService;
import com.azki.reservation.service.ReservationService.ReservationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Reserve and cancel the nearest available slot")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationMapper reservationMapper;

    public ReservationController(ReservationService reservationService,
                                 ReservationMapper reservationMapper) {
        this.reservationService = reservationService;
        this.reservationMapper = reservationMapper;
    }

    @Operation(summary = "Reserve the nearest available slot for the authenticated user")
    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody(required = false) ReservationRequest request) {

        LocalDateTime earliestFrom = request != null ? request.getEarliestFrom() : null;
        ReservationResult result = reservationService.reserveNearestSlot(currentUser.getId(), earliestFrom);

        return ResponseEntity.status(HttpStatus.CREATED).body(reservationMapper.toResponse(result));
    }

    @Operation(summary = "Cancel a reservation owned by the authenticated user")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id) {

        reservationService.cancelReservation(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List the nearest available slots")
    @GetMapping("/available")
    public ResponseEntity<List<AvailableSlotResponse>> listAvailable() {
        return ResponseEntity.ok(reservationService.listNearestAvailableSlots());
    }
}