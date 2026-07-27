package com.azki.reservation.service;

import com.azki.reservation.entity.AvailableSlot;
import com.azki.reservation.entity.Reservation;
import com.azki.reservation.entity.ReservationStatus;
import com.azki.reservation.exception.ForbiddenOperationException;
import com.azki.reservation.exception.NoAvailableSlotException;
import com.azki.reservation.exception.ReservationNotFoundException;
import com.azki.reservation.repository.AvailableSlotRepository;
import com.azki.reservation.repository.ReservationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReservationService {

    private final AvailableSlotRepository slotRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(AvailableSlotRepository slotRepository,
                              ReservationRepository reservationRepository) {
        this.slotRepository = slotRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResult reserveNearestSlot(Long userId, LocalDateTime earliestFrom) {
        LocalDateTime searchFrom = earliestFrom != null ? earliestFrom : LocalDateTime.now();

        AvailableSlot slot = slotRepository.findAndLockNearestAvailableSlot(searchFrom)
                .orElseThrow(() -> new NoAvailableSlotException("No available slot found"));

        slot.setReserved(true);
        slotRepository.save(slot);

        Reservation reservation = new Reservation();
        reservation.setSlotId(slot.getId());
        reservation.setUserId(userId);
        reservation.setStatus(ReservationStatus.ACTIVE);

        try {
            reservation = reservationRepository.save(reservation);
        } catch (DataIntegrityViolationException e) {
            throw new NoAvailableSlotException("This slot was just taken by another user, please try again");
        }

        return new ReservationResult(reservation, slot);
    }

    @Transactional
    public void cancelReservation(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("You are not allowed to cancel this reservation");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return;
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        slotRepository.findById(reservation.getSlotId()).ifPresent(slot -> {
            slot.setReserved(false);
            slotRepository.save(slot);
        });
    }

    public record ReservationResult(Reservation reservation, AvailableSlot slot) {
    }
}