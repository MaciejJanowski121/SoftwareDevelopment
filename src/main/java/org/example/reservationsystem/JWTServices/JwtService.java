package org.example.reservationsystem.JWTServices;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
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

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKeyBase64;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpiration;

    // --- API ---

    public String generateToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, jwtExpiration);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    /** BEZ try/catch – do zwykłego odczytu. Walidacja jest w isTokenValid() */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** BEZ try/catch – do zwykłego odczytu. Walidacja jest w isTokenValid() */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Odporna walidacja: zamiast propagować wyjątki (Expired, Malformed, itp.)
     * zwraca false, gdy token jest nieprawidłowy.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token); // może rzucić -> łapiemy niżej
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Dodatkowa walidacja “czy token w ogóle parsowalny i niewygasły”.
     * Pozostawiam – przydatne w kontrolerach/filtrach.
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token); // sprawdza podpis/format
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return extractUsername(token);
    }

    // --- prywatne ---

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts
                .builder()
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
            // traktujemy "nie da się odczytać" jako nieważny/wygaśnięty
            return true;
        }
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                // jeśli chcesz dać minimalny margines czasu:
                // .setAllowedClockSkewSeconds(1)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        // Używamy BASE64 (jak w Twoich testach ReflectionTestUtils.setField(...))
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        // Alternatywnie: Decoders.BASE64.decode(secretKeyBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}