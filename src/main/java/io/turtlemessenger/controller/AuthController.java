package io.turtlemessenger.controller;

import io.turtlemessenger.security.JwtUtil;
import io.turtlemessenger.service.AuthService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JwtUtil accessJwt;
    private final JwtUtil refreshJwt;
    private final AuthService authService;

    public AuthController(@Qualifier("accessJwt") JwtUtil accessJwt,
                          @Qualifier("refreshJwt") JwtUtil refreshJwt,
                          AuthService authService) {
        this.accessJwt = accessJwt;
        this.refreshJwt = refreshJwt;
        this.authService = authService;
    }

    public record LoginRequest(String username, String password) {}
    public record RegisterRequest(String username, String password) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            var user = authService.register(req.username(), req.password());
            String accessToken = accessJwt.generateToken(user.getUsername());
            String refreshToken = refreshJwt.generateToken(user.getUsername());
            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "username", user.getUsername()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            var user = authService.authenticate(req.username(), req.password());
            String accessToken = accessJwt.generateToken(user.getUsername());
            String refreshToken = refreshJwt.generateToken(user.getUsername());
            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "username", user.getUsername()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                     @RequestBody(required = false) Map<String, String> body) {
        String headerToken = null;
        String bodyToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            headerToken = authHeader.substring(7);
        }
        if (body != null) {
            bodyToken = body.get("refreshToken");
        }
        String tokenToUse = null;
        // Prefer a valid token, regardless of where it comes from
        if (headerToken != null && refreshJwt.isValid(headerToken)) {
            tokenToUse = headerToken;
        } else if (bodyToken != null && refreshJwt.isValid(bodyToken)) {
            tokenToUse = bodyToken;
        }
        if (tokenToUse == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid refresh token"));
        }
        String subject = refreshJwt.getSubject(tokenToUse);
        String newAccess = accessJwt.generateToken(subject);
        // For MVP we keep the same refresh token and return only a new access token
        return ResponseEntity.ok(Map.of("accessToken", newAccess));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("username", principal.getName()));
    }
}
