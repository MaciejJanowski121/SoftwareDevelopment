package org.example.reservationsystem.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/** Vereinheitlicht Fehlerrückgaben für das gesamte Backend. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Validierung @Valid (Body). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Eingaben sind ungültig. Bitte überprüfe die markierten Felder."
        );
        pd.setType(URI.create("https://docs.example/errors/validation"));
        pd.setInstance(URI.create(req.getRequestURI()));

        Map<String, String> fields = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), humanize(fe));
        }
        pd.setProperty("fields", fields);
        return pd;
    }

    /** Validierung @RequestParam / @PathVariable. */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Eingaben sind ungültig."
        );
        pd.setType(URI.create("https://docs.example/errors/validation"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /** Brak wymaganych parametrów. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Pflichtparameter fehlt: " + ex.getParameterName()
        );
        pd.setType(URI.create("https://docs.example/errors/missing-parameter"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /** Zły/nieparsowalny JSON. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Anfragekörper konnte nicht gelesen werden."
        );
        pd.setType(URI.create("https://docs.example/errors/bad-json"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /** Duplikaty (np. E-Mail już istnieje). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleConflict(DataIntegrityViolationException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Diese E-Mail ist bereits vergeben."
        );
        pd.setType(URI.create("https://docs.example/errors/email-taken"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /** Złe hasło / e-mail. */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "E-Mail oder Passwort ist falsch."
        );
        pd.setType(URI.create("https://docs.example/errors/bad-credentials"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /** Brak autoryzacji (inne błędy uwierzytelnienia). */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleAuth(AuthenticationException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Nicht autorisiert."
        );
        pd.setType(URI.create("https://docs.example/errors/unauthorized"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /** Brak uprawnień. */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Zugriff verweigert."
        );
        pd.setType(URI.create("https://docs.example/errors/forbidden"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /** Użytkownik nie istnieje. */
    @ExceptionHandler({ UsernameNotFoundException.class, UserNotFoundException.class, EntityNotFoundException.class })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(RuntimeException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage() != null ? ex.getMessage() : "Ressource nicht gefunden."
        );
        pd.setType(URI.create("https://docs.example/errors/not-found"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /** Reguły biznesowe / konflikt (np. „du hast bereits eine Reservierung”). */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage() != null ? ex.getMessage() : "Konflikt."
        );
        pd.setType(URI.create("https://docs.example/errors/business-rule"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /** Złe argumenty poza walidacją. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage() != null ? ex.getMessage() : "Ungültige Anfrage."
        );
        pd.setType(URI.create("https://docs.example/errors/invalid-argument"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /** Fallback. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unerwarteter Fehler. Bitte versuche es später erneut."
        );
        pd.setType(URI.create("https://docs.example/errors/unexpected"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    private static String humanize(FieldError e) {
        String code = e.getCode();
        if ("NotBlank".equals(code)) return "Dieses Feld darf nicht leer sein.";
        if ("Email".equals(code))    return "Bitte gib eine gültige E-Mail an.";
        if ("Size".equals(code))     return "Die Länge ist ungültig.";
        return e.getDefaultMessage() != null ? e.getDefaultMessage() : "Ungültiger Wert.";
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        pd.setType(URI.create("https://docs.example/errors/business-rule"));
        return pd;
    }

    @ExceptionHandler(TableAlreadyReservedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleTableReserved(TableAlreadyReservedException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://docs.example/errors/table-already-reserved"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }
    @ExceptionHandler(UserAlreadyHasReservationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleUserAlready(UserAlreadyHasReservationException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://docs.example/errors/user-has-reservation"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleReservationNotFound(ReservationNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage() != null ? ex.getMessage() : "Reservation not found."
        );
        pd.setType(URI.create("https://docs.example/errors/reservation-not-found"));
        return pd;
    }





}