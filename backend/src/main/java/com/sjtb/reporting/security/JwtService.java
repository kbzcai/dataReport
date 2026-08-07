package com.sjtb.reporting.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final Key signingKey;
    private final long expirationMinutes;
    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        if (secret.length() < 32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 characters");
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expirationMinutes = expirationMinutes;
    }
    public String createToken(UserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getUsername()).issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES))).signWith(signingKey).compact();
    }
    public String username(String token) { return claims(token).getSubject(); }
    public boolean valid(String token, UserDetails user) { return username(token).equals(user.getUsername()) && claims(token).getExpiration().after(new java.util.Date()); }
    private Claims claims(String token) { return Jwts.parser().verifyWith((javax.crypto.SecretKey) signingKey).build().parseSignedClaims(token).getPayload(); }
}
