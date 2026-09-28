package org.plishka.backend.service.auth.impl;

import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.plishka.backend.config.properties.JwtProperties;
import org.plishka.backend.domain.user.Role;
import org.plishka.backend.domain.user.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceImplTest {
    private static final String JWT_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY";
    private static final Long USER_ID = 42L;
    private static final String USER_EMAIL = "user@example.com";
    private static final String TAMPERED_SIGNATURE_SUFFIX = "tampered";

    @Test
    void generateAccessToken_ShouldCreateParseableTokenWithUserClaims() {
        JwtServiceImpl jwtService = jwtServiceAt(Instant.now(), Duration.ofMinutes(15));

        Claims claims = jwtService.parseClaims(jwtService.generateAccessToken(user())).orElseThrow();

        assertEquals(USER_EMAIL, claims.getSubject());
        assertEquals(USER_ID, claims.get("userId", Long.class));
    }

    @Test
    void parseClaims_ShouldReturnEmpty_WhenAccessTokenIsExpired() {
        JwtServiceImpl issuingJwtService = jwtServiceAt(
                Instant.now().minus(Duration.ofMinutes(5)),
                Duration.ofSeconds(1)
        );
        String token = issuingJwtService.generateAccessToken(user());

        JwtServiceImpl parsingJwtService = jwtServiceAt(Instant.now(), Duration.ofSeconds(1));

        assertTrue(parsingJwtService.parseClaims(token).isEmpty());
    }

    @Test
    void parseClaims_ShouldReturnEmpty_WhenTokenSignatureIsTampered() {
        JwtServiceImpl jwtService = jwtServiceAt(Instant.now(), Duration.ofMinutes(15));
        String token = jwtService.generateAccessToken(user());

        assertTrue(jwtService.parseClaims(tamperSignature(token)).isEmpty());
    }

    @Test
    void parseClaims_ShouldReturnEmpty_WhenTokenUsesNoneAlgorithm() {
        JwtServiceImpl jwtService = jwtServiceAt(Instant.now(), Duration.ofMinutes(15));

        assertTrue(jwtService.parseClaims(unsignedToken()).isEmpty());
    }

    private static JwtServiceImpl jwtServiceAt(Instant instant, Duration expiration) {
        return new JwtServiceImpl(
                new JwtProperties(JWT_SECRET, expiration),
                Clock.fixed(instant, ZoneOffset.UTC)
        );
    }

    private static String tamperSignature(String token) {
        return token + TAMPERED_SIGNATURE_SUFFIX;
    }

    private static String unsignedToken() {
        return base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}")
                + "."
                + base64Url("{\"sub\":\"" + USER_EMAIL + "\",\"userId\":" + USER_ID + "}")
                + ".";
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static User user() {
        Role role = new Role();
        role.setName(Role.RoleName.USER);

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(USER_EMAIL);
        user.setRoles(Set.of(role));
        return user;
    }
}
