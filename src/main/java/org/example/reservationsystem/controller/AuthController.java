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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

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
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(60L * 60 * 24)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthUserDTO> register(@Valid @RequestBody UserRegisterDTO dto,
                                                HttpServletResponse response) {
        try {
            String token = authService.register(dto);
            writeAuthCookie(response, token);

            // username == email (siehe Service)
            AuthUserDTO body = new AuthUserDTO(
                    dto.getEmail(),
                    "ROLE_USER",
                    dto.getFullName(),
                    dto.getEmail(),
                    dto.getPhone()
            );
            return ResponseEntity.ok(body);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginDTO dto,
                                   HttpServletResponse response) {
        try {
            String token = authService.login(dto);
            writeAuthCookie(response, token);

            User user = userRepository.findByEmail(dto.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            AuthUserDTO body = new AuthUserDTO(
                    user.getUsername(),       // = email
                    user.getRole().name(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone()
            );
            return ResponseEntity.ok(body);
        } catch (UsernameNotFoundException | BadCredentialsException e) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body("Login Failed");
        }

    }

    @GetMapping("/auth_check")
    public ResponseEntity<?> checkAuth(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Token");
        }

        String token = null;
        for (var c : cookies) {
            if ("token".equals(c.getName())) {
                token = c.getValue();
                break;
            }
        }
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Token");
        }

        // 1) Bez rzucania wyjątków — jeśli token zły/expired -> 401
        final String subject;
        try {
            subject = jwtService.getUsername(token); // u Ciebie to 'username' = email
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Token");
        }

        // 2) Szukaj najpierw po username, a jeśli pusto, to po email
        var userOpt = userRepository.findByUsername(subject);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(subject);
        }
        if (userOpt.isEmpty()) {
            // nie rzucamy UsernameNotFoundException, tylko 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Token");
        }

        var user = userOpt.get();
        var body = new AuthUserDTO(
                user.getUsername(),
                user.getRole().name(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone()
        );
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(false)      // true w HTTPS
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok().build();
    }
}