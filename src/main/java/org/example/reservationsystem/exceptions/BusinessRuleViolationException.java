package org.example.reservationsystem.exceptions;

public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}