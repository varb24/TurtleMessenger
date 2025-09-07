package io.turtlemessenger.config;

import io.turtlemessenger.security.JwtAuthFilter;
import io.turtlemessenger.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean(name = "accessJwt")
    public JwtUtil accessJwt() {
        String secret = System.getenv().getOrDefault("TM_JWT_SECRET", "dev-secret-change-me");
        long ttl = Long.parseLong(System.getenv().getOrDefault("TM_JWT_ACCESS_TTL_SECONDS", String.valueOf(15L * 60L))); // 15 minutes
        return new JwtUtil(secret, ttl);
    }

    @Bean(name = "refreshJwt")
    public JwtUtil refreshJwt() {
        String secret = System.getenv().getOrDefault("TM_JWT_SECRET", "dev-secret-change-me");
        long ttl = Long.parseLong(System.getenv().getOrDefault("TM_JWT_REFRESH_TTL_SECONDS", String.valueOf(7L * 24L * 60L * 60L))); // 7 days
        return new JwtUtil(secret, ttl);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, @org.springframework.beans.factory.annotation.Qualifier("accessJwt") JwtUtil jwt) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthFilter(jwt), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
