package com.shadiwaley.server.security;

import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiry-minutes}")
    private long accessTokenExpiryMinutes;

    public String generateCustomerAccessToken(UserAccount userAccount) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpiryMinutes * 60);

        return Jwts.builder()
                .subject(userAccount.getId().toString())
                .claim("actorType", ActorType.CUSTOMER.name())
                .claim("phone", userAccount.getPhone())
                .claim("side", userAccount.getSide().name())
                .claim("role", userAccount.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateEmployeeAccessToken(EmployeeAccount employeeAccount) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpiryMinutes * 60);

        return Jwts.builder()
                .subject(employeeAccount.getId().toString())
                .claim("actorType", ActorType.EMPLOYEE.name())
                .claim("email", employeeAccount.getEmail())
                .claim("role", employeeAccount.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    /*
     * Backward-compatible method for existing AuthService.
     */
    public String generateAccessToken(UserAccount userAccount) {
        return generateCustomerAccessToken(userAccount);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public ActorType extractActorType(String token) {
        String actorType = parseClaims(token).get("actorType", String.class);
        return ActorType.valueOf(actorType);
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        parseClaims(token);
        return true;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}