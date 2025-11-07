package org.example.reservationsystem.JWTServices;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * Servlet-Filter zur JWT-basierten Authentifizierung pro Request.
 *
 * <p>Der Filter extrahiert ein JWT bevorzugt aus dem {@code HttpOnly}-Cookie
 * namens {@code token}. Falls kein Cookie vorhanden ist, wird der
 * {@code Authorization}-Header im Format {@code Bearer &lt;jwt&gt;} ausgewertet.
 * Bei erfolgreicher Validierung wird ein {@link UsernamePasswordAuthenticationToken}
 * im {@link SecurityContextHolder} gesetzt, sodass nachfolgende Komponenten
 * den Benutzer als authentifiziert erkennen.</p>
 *
 * <p>Fehler (z. B. abgelaufenes/ungültiges Token) werden über den
 * {@link HandlerExceptionResolver} an die globale Fehlerbehandlung weitergereicht,
 * damit konsistente {@code ProblemDetail}-Antworten erzeugt werden.</p>
 *
 * <p>Der Filter wird in der Security-Kette vor dem
 * {@code UsernamePasswordAuthenticationFilter} registriert (siehe Security-Konfiguration),
 * und läuft dank {@link OncePerRequestFilter} genau einmal pro Anfrage.</p>
 *
 * @author Maciej Janowski
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Erstellt einen neuen {@code JwtAuthenticationFilter}.
     *
     * @param jwtService               Dienst zum Extrahieren/Validieren von JWTs
     * @param userDetailsService       Dienst zum Laden von {@link UserDetails} anhand der E-Mail
     * @param handlerExceptionResolver Resolver zur Übergabe von Ausnahmen an das globale Handling
     */
    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    /**
     * Führt die Filterlogik aus: Token extrahieren → validieren → Authentifizierung setzen.
     *
     * <ol>
     *   <li>Versucht, das JWT aus dem Cookie {@code token} zu lesen.</li>
     *   <li>Fällt auf {@code Authorization: Bearer &lt;jwt&gt;} zurück, wenn kein Cookie gesetzt ist.</li>
     *   <li>Ist kein Token vorhanden, wird die Kette ohne Änderung fortgesetzt.</li>
     *   <li>Ist ein Token vorhanden, wird der Benutzername (Subject) extrahiert und
     *       das zugehörige {@link UserDetails} geladen.</li>
     *   <li>Bei gültigem Token wird ein {@link UsernamePasswordAuthenticationToken} erzeugt
     *       und im {@link SecurityContextHolder} abgelegt.</li>
     *   <li>Anschließend wird die Filterkette fortgesetzt.</li>
     * </ol>
     *
     * <p>Tritt während der Verarbeitung eine Ausnahme auf (z. B. ungültiges Token),
     * wird sie an den {@link HandlerExceptionResolver} delegiert, um konsistente
     * Fehlerantworten zu erzeugen.</p>
     *
     * @param request  aktueller HTTP-Request
     * @param response aktuelle HTTP-Response
     * @param filterChain restliche Filterkette
     * @throws ServletException bei Servlet-Fehlern
     * @throws IOException      bei I/O-Fehlern
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String jwt = null;

        // 1) Token aus HttpOnly-Cookie "token"
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (cookie.getName().equals("token")) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        // 2) Fallback: Authorization-Header "Bearer <jwt>"
        if (jwt == null) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
            }
        }

        // 3) Ohne Token → weiter in der Kette
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtService.extractUsername(jwt);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 4) Nur setzen, wenn noch nicht authentifiziert
            if (username != null && authentication == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            // 5) Immer fortsetzen
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            // Delegation an globale Fehlerbehandlung (ProblemDetail etc.)
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}