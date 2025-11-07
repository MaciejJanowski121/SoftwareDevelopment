package org.example.reservationsystem.exceptions;


public class TableAlreadyReservedException extends RuntimeException {
    public TableAlreadyReservedException(int tableNumber) {
        super("Dieser Tisch ist in dem gewählten Zeitraum bereits reserviert.");
    }
}