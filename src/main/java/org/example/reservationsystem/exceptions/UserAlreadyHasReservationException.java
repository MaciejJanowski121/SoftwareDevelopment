package org.example.reservationsystem.exceptions;

public class UserAlreadyHasReservationException extends RuntimeException {
    public UserAlreadyHasReservationException() {
        super("Du hast bereits eine aktive Reservierung.");
    }
}