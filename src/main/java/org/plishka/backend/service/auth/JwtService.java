package org.plishka.backend.service.auth;

import io.jsonwebtoken.Claims;
import java.util.Optional;
import org.plishka.backend.domain.user.User;

public interface JwtService {
    String generateAccessToken(User user);

    Optional<Claims> parseClaims(String token);

    String extractEmail(Claims claims);
}
