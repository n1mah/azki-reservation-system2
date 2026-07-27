package com.azki.reservation.service;

import com.azki.reservation.entity.AvailableSlot;
import com.azki.reservation.entity.Reservation;
import com.azki.reservation.entity.ReservationStatus;
import com.azki.reservation.exception.ForbiddenOperationException;
import com.azki.reservation.exception.NoAvailableSlotException;
import com.azki.reservation.exception.ReservationNotFoundException;
import com.azki.reservation.repository.AvailableSlotRepository;
import com.azki.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;

    @Mock
    private AvailableSlotRepository slotRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    @DisplayName("Reserving when no slot is available throws NoAvailableSlotException")
    void reserveWithoutAvailableSlot() {
        when(slotRepository.findAndLockNearestAvailableSlot(any()))
                .thenReturn(Optional.empty());

        assertThrows(NoAvailableSlotException.class,
                () -> reservationService.reserveNearestSlot(USER_ID, null));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reserving marks the slot as taken and creates an active reservation")
    void reserveSucceeds() {
        AvailableSlot slot = slot(42L);
        Reservation saved = reservation(7L, 42L, USER_ID, ReservationStatus.ACTIVE);

        when(slotRepository.findAndLockNearestAvailableSlot(any()))
                .thenReturn(Optional.of(slot));
        when(slotRepository.save(any(AvailableSlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(Reservation.class)))
                .thenReturn(saved);

        ReservationService.ReservationResult result =
                reservationService.reserveNearestSlot(USER_ID, null);

        assertEquals(7L, result.reservation().getId());
        assertEquals(42L, result.slot().getId());
        assertTrue(slot.isReserved());
        verify(slotRepository).save(slot);
    }

    @Test
    @DisplayName("Cancelling a non-existent reservation throws ReservationNotFoundException")
    void cancelMissingReservation() {
        when(reservationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class,
                () -> reservationService.cancelReservation(999L, USER_ID));

        verify(slotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancelling another user's reservation throws ForbiddenOperationException")
    void cancelForeignReservation() {
        Reservation reservation = reservation(5L, 10L, USER_ID, ReservationStatus.ACTIVE);

        when(reservationRepository.findById(5L))
                .thenReturn(Optional.of(reservation));

        assertThrows(ForbiddenOperationException.class,
                () -> reservationService.cancelReservation(5L, OTHER_USER_ID));

        verify(reservationRepository, never()).save(any());
        verify(slotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancelling an already cancelled reservation is a no-op")
    void cancelAlreadyCancelledReservation() {
        Reservation reservation = reservation(6L, 11L, USER_ID, ReservationStatus.CANCELLED);

        when(reservationRepository.findById(6L))
                .thenReturn(Optional.of(reservation));

        reservationService.cancelReservation(6L, USER_ID);

        verify(reservationRepository, never()).save(any());
        verify(slotRepository, never()).save(any());
    }

    private AvailableSlot slot(Long id) {
        AvailableSlot slot = new AvailableSlot();
        slot.setId(id);
        slot.setStartTime(LocalDateTime.now().plusHours(1));
        slot.setEndTime(LocalDateTime.now().plusHours(2));
        slot.setReserved(false);
        return slot;
    }

    private Reservation reservation(Long id, Long slotId, Long userId, ReservationStatus status) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setSlotId(slotId);
        reservation.setUserId(userId);
        reservation.setStatus(status);
        return reservation;
    }
}