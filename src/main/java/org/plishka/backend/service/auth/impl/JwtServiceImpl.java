package org.plishka.backend.service.auth.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.user.Role;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.service.auth.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtServiceImpl implements JwtService {
    private static final String ROLES_CLAIM = "roles";
    private static final String USER_ID_CLAIM = "userId";

    private final SecretKey signingKey;
    private final Duration accessTokenExpiration;
    private final Clock clock;

    public JwtServiceImpl(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") Duration accessTokenExpiration,
            Clock clock
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.accessTokenExpiration = accessTokenExpiration;
        this.clock = clock;
    }

    @Override
    public String generateAccessToken(User user) {
        Instant now = Instant.now(clock);

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .toList();

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(USER_ID_CLAIM, user.getUserId())
                .claim(ROLES_CLAIM, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiration)))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public Optional<Claims> parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(claims);
        } catch (SecurityException e) {
            log.warn("JWT signature validation failed");
        } catch (MalformedJwtException e) {
            log.debug("JWT token is malformed");
        } catch (ExpiredJwtException e) {
            log.debug("JWT token is expired");
        } catch (UnsupportedJwtException e) {
            log.debug("JWT token is unsupported");
        } catch (IllegalArgumentException e) {
            log.debug("JWT token is empty or blank");
        }

        return Optional.empty();
    }

    @Override
    public String extractEmail(Claims claims) {
        return claims.getSubject();
    }
}
