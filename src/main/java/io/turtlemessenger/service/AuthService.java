package io.turtlemessenger.service;

import io.turtlemessenger.model.UserAccount;
import io.turtlemessenger.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public UserAccount register(String username, String rawPassword) {
        String u = normalize(username);
        if (u.length() < 3 || u.length() > 50 || !u.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException("invalid username; use a-z, 0-9, . _ - (3-50 chars)");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("password must be at least 6 characters");
        }
        if (users.existsByUsername(u)) {
            throw new IllegalArgumentException("username already taken");
        }
        String hash = encoder.encode(rawPassword);
        UserAccount acc = new UserAccount(u, hash);
        return users.save(acc);
    }

    public UserAccount authenticate(String username, String rawPassword) {
        String u = normalize(username);
        UserAccount acc = users.findByUsername(u).orElseThrow(() -> new IllegalArgumentException("invalid credentials"));
        if (!encoder.matches(rawPassword, acc.getPasswordHash())) {
            throw new IllegalArgumentException("invalid credentials");
        }
        return acc;
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}

