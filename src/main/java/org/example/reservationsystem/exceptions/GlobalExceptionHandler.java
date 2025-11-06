package org.example.reservationsystem.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Eingaben sind ungültig. Bitte überprüfe die markierten Felder."
        );
        pd.setType(URI.create("https://docs.example/errors/validation"));

        Map<String, String> fields = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), humanize(fe));
        }
        pd.setProperty("fields", fields);
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleConflict(DataIntegrityViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Diese E-Mail ist bereits vergeben."
        );
        pd.setType(URI.create("https://docs.example/errors/email-taken"));
        return pd;
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "E-Mail oder Passwort ist falsch."
        );
        pd.setType(URI.create("https://docs.example/errors/bad-credentials"));
        return pd;
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleUserNotFound(UsernameNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Benutzer wurde nicht gefunden."
        );
        pd.setType(URI.create("https://docs.example/errors/user-not-found"));
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage() != null ? ex.getMessage() : "Anfrage konnte nicht verarbeitet werden."
        );
        pd.setType(URI.create("https://docs.example/errors/business-rule"));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unerwarteter Fehler. Bitte versuche es später erneut."
        );
        pd.setType(URI.create("https://docs.example/errors/unexpected"));
        return pd;
    }

    private static String humanize(FieldError e) {
        String code = e.getCode();
        if ("NotBlank".equals(code)) return "Dieses Feld darf nicht leer sein.";
        if ("Email".equals(code))    return "Bitte gib eine gültige E-Mail an.";
        if ("Size".equals(code))     return "Die Länge ist ungültig.";
        return e.getDefaultMessage() != null ? e.getDefaultMessage() : "Ungültiger Wert.";
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleCustomUserNotFound(UserNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage() != null ? ex.getMessage() : "Benutzer wurde nicht gefunden."
        );
        pd.setType(URI.create("https://docs.example/errors/user-not-found"));
        return pd;
    }

    @ExceptionHandler(TableNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleTableNotFound(TableNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage() != null ? ex.getMessage() : "Tisch wurde nicht gefunden."
        );
        pd.setType(URI.create("https://docs.example/errors/table-not-found"));
        return pd;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage() != null ? ex.getMessage() : "Ressource nicht gefunden."
        );
        pd.setType(URI.create("https://docs.example/errors/not-found"));
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage() != null ? ex.getMessage() : "Ungültige Anfrage."
        );
        pd.setType(URI.create("https://docs.example/errors/invalid-argument"));
        return pd;
    }
}