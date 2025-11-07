package org.example.reservationsystem.JWTServices;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Dienstklasse für das Erstellen, Validieren und Auslesen von JSON Web Tokens (JWT).
 *
 * <p>Diese Klasse kapselt alle sicherheitsrelevanten Operationen rund um JWTs
 * und wird sowohl im {@link org.example.reservationsystem.JWTServices.JwtAuthenticationFilter}
 * als auch im {@code AuthService} verwendet.</p>
 *
 * <p>Das JWT wird symmetrisch mit dem in der Anwendungskonfiguration hinterlegten
 * Secret Key (HS256) signiert und enthält als Subject die E-Mail-Adresse
 * des Benutzers (Login-Identifier).</p>
 *
 * <p>Standardmäßige Ablaufzeit: 1 Stunde (3600000 ms), konfigurierbar über
 * {@code application.properties}:</p>
 * <pre>{@code
 * jwt.secret=BASE64_ENCODED_SECRET
 * jwt.expiration=3600000
 * }</pre>
 *
 * @author Maciej Janowski
 */
@Service
public class JwtService {

    /** Geheimschlüssel in Base64-kodierter Form (aus application.properties). */
    @Value("${jwt.secret}")
    private String secretKeyBase64;

    /** Token-Gültigkeitsdauer in Millisekunden (Standard: 1 h). */
    @Value("${jwt.expiration:3600000}")
    private long jwtExpiration;

    /**
     * Erzeugt ein neues signiertes JWT für den angegebenen Benutzer.
     *
     * @param userDetails Benutzer, dessen E-Mail als Subject gesetzt wird
     * @return signiertes JWT
     */
    public String generateToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, jwtExpiration);
    }

    /** @return aktuelle Ablaufzeit in Millisekunden */
    public long getExpirationTime() {
        return jwtExpiration;
    }

    /**
     * Extrahiert den Benutzernamen (Subject) aus einem gültigen Token.
     *
     * <p>Diese Methode führt keine Validierung durch — sie geht davon aus,
     * dass das Token bereits geprüft wurde. Bei ungültigem oder abgelaufenem
     * Token wird eine {@link JwtException} ausgelöst.</p>
     *
     * @param token JWT
     * @return Benutzername (E-Mail)
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Liest einen spezifischen Claim (z. B. Ablaufdatum) aus dem Token.
     *
     * @param token          JWT
     * @param claimsResolver Funktion zur Extraktion eines Werts aus den Claims
     * @param <T>            Typ des zurückgegebenen Werts
     * @return extrahierter Wert
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Führt eine robuste Validierung des Tokens durch.
     *
     * <p>Prüft Signatur, Ablaufzeit und ob das Subject (E-Mail) mit dem
     * im {@link UserDetails} gespeicherten Benutzernamen übereinstimmt.
     * Bei Fehlern wird {@code false} zurückgegeben, anstatt eine Ausnahme zu werfen.</p>
     *
     * @param token       JWT-String
     * @param userDetails Benutzer, dessen E-Mail als Vergleich dient
     * @return {@code true}, wenn das Token gültig ist; sonst {@code false}
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Prüft, ob das Token formal gültig und noch nicht abgelaufen ist.
     *
     * @param token JWT
     * @return {@code true}, wenn gültig und nicht abgelaufen
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token); // prüft Signatur und Struktur
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Alias für {@link #extractUsername(String)} */
    public String getUsername(String token) {
        return extractUsername(token);
    }



    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true; // nicht lesbar oder ungültig -> gilt als abgelaufen
        }
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Wandelt den Base64-kodierten Schlüssel in ein {@link Key}-Objekt um,
     * das für die HMAC-Signatur (HS256) verwendet wird.
     *
     * @return HMAC-Signaturschlüssel
     */
    private Key getSignInKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}