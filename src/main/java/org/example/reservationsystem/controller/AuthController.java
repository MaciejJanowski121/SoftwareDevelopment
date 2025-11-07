package org.example.reservationsystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.reservationsystem.DTO.AuthUserDTO;
import org.example.reservationsystem.DTO.UserLoginDTO;
import org.example.reservationsystem.DTO.UserRegisterDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService,
                          JwtService jwtService,
                          UserRepository userRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    private void writeAuthCookie(HttpServletResponse response, String token) {
        long maxAgeSeconds = Duration.ofMillis(jwtService.getExpirationTime()).getSeconds();
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String extractToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var c : request.getCookies()) {
                if ("token".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthUserDTO> register(
            @Valid @RequestBody UserRegisterDTO dto,
            HttpServletResponse response
    ) {
        String token = authService.register(dto);
        writeAuthCookie(response, token);

        User saved = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found after registration"));

        return ResponseEntity.ok(AuthUserDTO.fromUser(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthUserDTO> login(
            @Valid @RequestBody UserLoginDTO dto,
            HttpServletResponse response
    ) {
        String token = authService.login(dto);
        writeAuthCookie(response, token);

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return ResponseEntity.ok(AuthUserDTO.fromUser(user));
    }

    @GetMapping("/auth_check")
    public ResponseEntity<?> checkAuth(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Token");
        }

        String email = jwtService.getUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return ResponseEntity.ok(AuthUserDTO.fromUser(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok().build();
    }
}