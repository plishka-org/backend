package org.plishka.backend.service.auth.impl;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.user.EmailVerificationToken;
import org.plishka.backend.domain.user.RefreshToken;
import org.plishka.backend.domain.user.Role;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.auth.AuthResponseDto;
import org.plishka.backend.dto.auth.LoginRequestDto;
import org.plishka.backend.dto.auth.RegisterRequestDto;
import org.plishka.backend.event.auth.EmailVerificationRequestedEvent;
import org.plishka.backend.exception.AuthenticationFailedException;
import org.plishka.backend.exception.EmailAlreadyExistsException;
import org.plishka.backend.exception.EmailNotVerifiedException;
import org.plishka.backend.exception.ForbiddenException;
import org.plishka.backend.exception.InvalidVerificationTokenException;
import org.plishka.backend.exception.RefreshTokenDeviceMismatchException;
import org.plishka.backend.exception.RefreshTokenExpiredException;
import org.plishka.backend.exception.RefreshTokenNotFoundException;
import org.plishka.backend.repository.user.EmailVerificationTokenRepository;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.RoleRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.auth.AuthService;
import org.plishka.backend.service.auth.JwtService;
import org.plishka.backend.util.TokenGenerator;
import org.plishka.backend.util.TokenHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    public static final int EMAIL_VERIFICATION_TOKEN_TTL_HOURS = 24;
    private static final String VERIFY_TOKEN_PATH = "/auth/verify?token=";
    private static final String USERS_EMAIL_CONSTRAINT = "uk_users_email";

    @Value("${backend.refresh-token-expiration}")
    private Duration refreshTokenExpiration;

    @Value("${backend.base-url}")
    private String backendBaseUrl;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public void register(RegisterRequestDto request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = buildUser(request, normalizedEmail);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isUsersEmailUniqueConstraintViolation(exception)) {
                throw new EmailAlreadyExistsException(
                        "User with email '%s' already exists".formatted(normalizedEmail)
                );
            }
            throw exception;
        }

        String rawVerificationToken = TokenGenerator.generateEmailVerificationToken();
        emailVerificationTokenRepository.save(buildEmailVerificationToken(user, rawVerificationToken));

        applicationEventPublisher.publishEvent(
                new EmailVerificationRequestedEvent(user.getEmail(), buildVerificationLink(rawVerificationToken))
        );

        log.info("User registered: userId={}, email={}", user.getUserId(), user.getEmail());
    }

    @Override
    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken verificationToken = findEmailVerificationTokenOrThrow(rawToken);

        User user = userRepository.findByIdForUpdate(verificationToken.getUser().getUserId())
                .orElseThrow(() -> new InvalidVerificationTokenException("User not found"));

        if (user.isEmailVerified()) {
            emailVerificationTokenRepository.deleteAllByUser(user);
            return;
        }

        if (verificationToken.getExpiresAt().isBefore(Instant.now(clock))) {
            throw new InvalidVerificationTokenException("Verification token has expired");
        }

        user.setEmailVerified(true);
        emailVerificationTokenRepository.deleteAllByUser(user);

        log.info("Email verified successfully, userId={}", user.getUserId());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        userRepository.findByEmail(normalizedEmail)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> {
                    emailVerificationTokenRepository.deleteAllByUser(user);

                    String rawVerificationToken = TokenGenerator.generateEmailVerificationToken();
                    EmailVerificationToken emailVerificationToken =
                            buildEmailVerificationToken(user, rawVerificationToken);

                    emailVerificationTokenRepository.save(emailVerificationToken);

                    String verificationLink = buildVerificationLink(rawVerificationToken);
                    applicationEventPublisher.publishEvent(
                            new EmailVerificationRequestedEvent(user.getEmail(), verificationLink)
                    );

                    log.info(
                            "Verification email resent: email={}, userId={}",
                            user.getEmail(),
                            user.getUserId()
                    );
                });
    }

    @Override
    @Transactional
    public AuthResponseDto login(LoginRequestDto request, String deviceId) {
        String normalizedEmail = normalizeEmail(request.email());

        User user = findUserByEmailOrThrow(normalizedEmail);
        validatePasswordOrThrow(request.password(), user);

        User lockedUser = findUserByIdForUpdateOrThrow(user.getUserId());
        if (!lockedUser.getPasswordHash().equals(user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }
        validateUserCanAuthenticateOrThrow(lockedUser);

        String normalizedDeviceId = normalizeDeviceId(deviceId);
        refreshTokenRepository.deleteAllByUserIdAndDeviceId(lockedUser.getUserId(), normalizedDeviceId);
        refreshTokenRepository.flush();

        final String accessToken = jwtService.generateAccessToken(lockedUser);
        final String rawRefreshToken = TokenGenerator.generateRefreshToken();

        persistRefreshToken(lockedUser, rawRefreshToken, normalizedDeviceId);

        log.info(
                "Login successful: userId={}, email={}, deviceId={}",
                lockedUser.getUserId(),
                lockedUser.getEmail(),
                normalizedDeviceId
        );

        return new AuthResponseDto(accessToken, rawRefreshToken);
    }

    @Override
    @Transactional
    public AuthResponseDto refresh(String rawRefreshToken, String deviceId) {
        String normalizedDeviceId = normalizeDeviceId(deviceId);

        RefreshToken currentToken = findRefreshTokenForUpdateOrThrow(rawRefreshToken);
        validateRefreshTokenOrThrow(currentToken, normalizedDeviceId);

        User lockedUser = findUserByIdForUpdateOrThrow(currentToken.getUser().getUserId());
        validateUserCanAuthenticateOrThrow(lockedUser);

        refreshTokenRepository.deleteAllByUserIdAndDeviceId(lockedUser.getUserId(), normalizedDeviceId);
        refreshTokenRepository.flush();

        final String newAccessToken = jwtService.generateAccessToken(lockedUser);
        final String newRefreshToken = TokenGenerator.generateRefreshToken();

        persistRefreshToken(lockedUser, newRefreshToken, normalizedDeviceId);

        log.info(
                "Token refresh successful: userId={}, email={}, deviceId={}",
                lockedUser.getUserId(),
                lockedUser.getEmail(),
                normalizedDeviceId
        );

        return new AuthResponseDto(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logoutCurrentDevice(Long userId, String deviceId) {
        String normalizedDeviceId = normalizeDeviceId(deviceId);

        int deletedTokens = refreshTokenRepository.deleteAllByUserIdAndDeviceId(
                userId,
                normalizedDeviceId
        );

        log.info(
                "Logout current device: userId={}, deviceId={}, deletedTokens={}",
                userId,
                normalizedDeviceId,
                deletedTokens
        );
    }

    private User buildUser(RegisterRequestDto request, String normalizedEmail) {
        return User.builder()
                .name(normalizeName(request.name()))
                .email(normalizedEmail)
                .phone(normalizePhone(request.phone()))
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(Set.of(
                        roleRepository.findByName(Role.RoleName.USER)
                                .orElseThrow(() -> new IllegalStateException("Role USER not found"))
                ))
                .isBanned(false)
                .isEmailVerified(false)
                .build();
    }

    private EmailVerificationToken buildEmailVerificationToken(User user, String rawToken) {
        return EmailVerificationToken.builder()
                .tokenHash(TokenHashUtil.sha256(rawToken))
                .user(user)
                .expiresAt(now().plus(Duration.ofHours(EMAIL_VERIFICATION_TOKEN_TTL_HOURS)))
                .build();
    }

    private void persistRefreshToken(User user, String rawToken, String deviceId) {
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(TokenHashUtil.sha256(rawToken))
                .user(user)
                .deviceId(deviceId)
                .expiresAt(now().plus(refreshTokenExpiration))
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    private User findUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));
    }

    private User findUserByIdForUpdateOrThrow(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthenticationFailedException("Authentication failed"));
    }

    private RefreshToken findRefreshTokenForUpdateOrThrow(String rawRefreshToken) {
        String refreshTokenHash = TokenHashUtil.sha256(rawRefreshToken);

        return refreshTokenRepository.findByTokenHashForUpdate(refreshTokenHash)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token not found"));
    }

    private EmailVerificationToken findEmailVerificationTokenOrThrow(String rawToken) {
        String verificationTokenHash = TokenHashUtil.sha256(rawToken);

        return emailVerificationTokenRepository.findByTokenHash(verificationTokenHash)
                .orElseThrow(() -> new InvalidVerificationTokenException("Verification token not found"));
    }

    private void validateRefreshTokenOrThrow(RefreshToken refreshToken, String deviceId) {
        if (refreshToken.getExpiresAt().isBefore(now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        if (!refreshToken.getDeviceId().equals(deviceId)) {
            throw new RefreshTokenDeviceMismatchException(
                    "Refresh token does not belong to this device"
            );
        }
    }

    private void validateUserCanAuthenticateOrThrow(User user) {
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email is not verified");
        }

        if (user.isBanned()) {
            throw new ForbiddenException("User is banned");
        }
    }

    private void validatePasswordOrThrow(String rawPassword, User user) {
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }
    }

    private boolean isUsersEmailUniqueConstraintViolation(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException cve
                    && USERS_EMAIL_CONSTRAINT.equalsIgnoreCase(cve.getConstraintName())) {
                return true;
            }

            String message = cause.getMessage();
            if (message != null && message.contains(USERS_EMAIL_CONSTRAINT)) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private String buildVerificationLink(String rawVerificationToken) {
        return backendBaseUrl + VERIFY_TOKEN_PATH + rawVerificationToken;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }

    private String normalizeDeviceId(String deviceId) {
        return UUID.fromString(deviceId.trim()).toString();
    }

    private String normalizeName(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFC).trim();
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
