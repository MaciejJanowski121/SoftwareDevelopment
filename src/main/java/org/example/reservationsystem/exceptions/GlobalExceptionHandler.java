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

/**
 * Globaler Exception-Handler für das gesamte Backend.
 *
 * <p>Diese Klasse vereinheitlicht Fehlerantworten und wandelt Ausnahmen in
 * standardisierte {@link ProblemDetail}-Objekte (RFC 7807) um. Dabei werden
 * sinnvolle HTTP-Statuscodes gesetzt, eine maschinenlesbare {@code type}-URI
 * vergeben sowie – wo sinnvoll – zusätzliche Eigenschaften (z. B. Feldfehler)
 * ergänzt.</p>
 *
 * <p>Vorteile:
 * <ul>
 *   <li>Konsistente Fehlerformate für Frontend/Client.</li>
 *   <li>Klare Abbildung von Domänenfehlern auf HTTP (Status, Typ, Detail, Instance).</li>
 *   <li>Gute Erweiterbarkeit für projektspezifische Ausnahmen.</li>
 * </ul>
 * </p>
 *
 * <p>Hinweis: {@code instance} wird mit der anfragten Request-URI befüllt, damit
 * Clients den Kontext des Fehlers nachvollziehen können.</p>
 *
 * @author Maciej Janowski
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Validierungsfehler für {@code @Valid}-annotierte Request-Bodies.
     *
     * <p>Sammelt Feldfehler in einer Map {@code fields} (Feldname → menschenlesbare Nachricht)
     * und liefert {@code 400 Bad Request}.</p>
     *
     * @param ex  Auslöser {@link MethodArgumentNotValidException}
     * @param req aktueller HTTP-Request (für {@code instance})
     * @return {@link ProblemDetail} mit Typ {@code /errors/validation} und Feldfehlern
     */
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

    /**
     * Validierungsfehler für {@code @RequestParam}/{@code @PathVariable} (Bean Validation).
     *
     * <p>Liefert {@code 400 Bad Request} mit allgemeinem Validierungshinweis.</p>
     *
     * @param ex  {@link ConstraintViolationException}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Typ {@code /errors/validation}
     */
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

    /**
     * Fehlender Pflichtparameter ({@code @RequestParam}).
     *
     * <p>Liefert {@code 400 Bad Request} mit dem fehlenden Parameternamen.</p>
     *
     * @param ex  {@link MissingServletRequestParameterException}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Typ {@code /errors/missing-parameter}
     */
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

    /**
     * Ungültiger oder nicht lesbarer Request-Body (z. B. defektes JSON).
     *
     * <p>Liefert {@code 400 Bad Request}.</p>
     *
     * @param ex  {@link HttpMessageNotReadableException}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Typ {@code /errors/bad-json}
     */
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

    /**
     * Datenintegritätsverletzung (z. B. E-Mail bereits vergeben).
     *
     * <p>Liefert {@code 409 Conflict}.</p>
     *
     * @param ex  {@link DataIntegrityViolationException}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Typ {@code /errors/email-taken}
     */
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

    /**
     * Ungültige Zugangsdaten (falsche E-Mail/Passwort).
     *
     * <p>Liefert {@code 401 Unauthorized}.</p>
     *
     * @param ex  {@link BadCredentialsException}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Typ {@code /errors/bad-credentials}
     */
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

    /**
     * Allgemeine Authentifizierungsfehler (kein/ungültiges Token etc.).
     *
     * <p>Liefert {@code 401 Unauthorized}.</p>
     *
     * @param ex  {@link AuthenticationException}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Typ {@code /errors/unauthorized}
     */
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

    /**
     * Fehlende Berechtigung (Autorisierung verweigert).
     *
     * <p>Liefert {@code 403 Forbidden}.</p>
     *
     * @param ex  {@link AccessDeniedException}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Typ {@code /errors/forbidden}
     */
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

    /**
     * Ressourcen nicht gefunden (Benutzer/Entität).
     *
     * <p>Fasst {@link UsernameNotFoundException}, eigene {@code UserNotFoundException}
     * sowie {@link EntityNotFoundException} zusammen. Liefert {@code 404 Not Found}.</p>
     *
     * @param ex  konkrete {@link RuntimeException}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Typ {@code /errors/not-found}
     */
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

    /**
     * Domänen-/Geschäftsregelkonflikte (z. B. „Benutzer hat bereits eine Reservierung“).
     *
     * <p>Liefert {@code 409 Conflict}.</p>
     *
     * @param ex  {@link IllegalStateException}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Typ {@code /errors/business-rule}
     */
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

    /**
     * Ungültige Argumente außerhalb Bean Validation.
     *
     * <p>Liefert {@code 400 Bad Request}.</p>
     *
     * @param ex  {@link IllegalArgumentException}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Typ {@code /errors/invalid-argument}
     */
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

    /**
     * Generischer Fallback für nicht speziell behandelte Fehler.
     *
     * <p>Liefert {@code 500 Internal Server Error} mit einer generischen Nachricht,
     * ohne interne Details preiszugeben.</p>
     *
     * @param ex  unbekannte {@link Exception}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Typ {@code /errors/unexpected}
     */
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

    /**
     * Wandelt einen {@link FieldError} in eine menschenlesbare Meldung um.
     *
     * @param e Feldfehler
     * @return lokalisierte/vereinheitlichte Fehlermeldung
     */
    private static String humanize(FieldError e) {
        String code = e.getCode();
        if ("NotBlank".equals(code)) return "Dieses Feld darf nicht leer sein.";
        if ("Email".equals(code))    return "Bitte gib eine gültige E-Mail an.";
        if ("Size".equals(code))     return "Die Länge ist ungültig.";
        return e.getDefaultMessage() != null ? e.getDefaultMessage() : "Ungültiger Wert.";
    }

    /**
     * Domänenspezifischer Verstoß gegen eine Geschäftsregel.
     *
     * <p>Liefert {@code 400 Bad Request} mit Typ {@code /errors/business-rule}.</p>
     *
     * @param ex {@link BusinessRuleViolationException}
     * @return {@link ProblemDetail} mit Regelbeschreibung aus der Exception
     */
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

    /**
     * Spezieller Konflikt: Tisch bereits reserviert.
     *
     * <p>Liefert {@code 409 Conflict} mit Typ {@code /errors/table-already-reserved}.</p>
     *
     * @param ex  {@link TableAlreadyReservedException}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Konfliktbeschreibung
     */
    @ExceptionHandler(TableAlreadyReservedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleTableReserved(TableAlreadyReservedException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://docs.example/errors/table-already-reserved"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /**
     * Spezieller Konflikt: Benutzer hat bereits eine aktive Reservierung (1:1-Beziehung).
     *
     * <p>Liefert {@code 409 Conflict} mit Typ {@code /errors/user-has-reservation}.</p>
     *
     * @param ex  {@link UserAlreadyHasReservationException}
     * @param req aktueller HTTP-Request
     * @return {@link ProblemDetail} mit Konfliktbeschreibung
     */
    @ExceptionHandler(UserAlreadyHasReservationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleUserAlready(UserAlreadyHasReservationException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://docs.example/errors/user-has-reservation"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /**
     * Spezieller Not-Found-Fall: Reservierung nicht gefunden.
     *
     * <p>Liefert {@code 404 Not Found} mit Typ {@code /errors/reservation-not-found}.</p>
     *
     * @param ex {@link ReservationNotFoundException}
     * @return {@link ProblemDetail} mit Fehlermeldung
     */
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