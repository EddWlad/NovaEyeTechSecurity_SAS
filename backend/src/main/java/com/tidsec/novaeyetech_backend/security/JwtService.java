package com.tidsec.novaeyetech_backend.security;

import com.tidsec.novaeyetech_backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Emision y verificacion del JWT.
 *
 * <p>Los claims replican los del backend NestJS ({@code sub}, {@code email}, {@code role}) para que
 * cualquier cliente ya integrado siga leyendo el token igual.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    /** HS256 exige al menos 256 bits de clave. */
    private static final int MIN_SECRET_LENGTH = 32;

    private final JwtProperties properties;
    private SecretKey signingKey;

    @PostConstruct
    void initialize() {
        String secret = properties.secret();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Falta JWT_SECRET: el backend no puede firmar tokens. "
                            + "Copiar .env.example a .env y poner un secreto propio, "
                            + "o definir la variable de entorno JWT_SECRET.");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos " + MIN_SECRET_LENGTH + " caracteres; "
                            + "llegaron " + secret.length() + ".");
        }

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.expiration())))
                .signWith(signingKey)
                .compact();
    }

    /** Devuelve los claims solo si la firma y la expiracion son validas. */
    public Optional<Claims> parseToken(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long expirationSeconds() {
        return properties.expiration().toSeconds();
    }
}
