package org.plishka.backend.service.auth.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.config.properties.FrontendProperties;
import org.plishka.backend.domain.user.EmailVerificationToken;
import org.plishka.backend.domain.user.PasswordResetToken;
import org.plishka.backend.domain.user.RefreshToken;
import org.plishka.backend.domain.user.Role;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.auth.AuthResponseDto;
import org.plishka.backend.dto.auth.LoginRequestDto;
import org.plishka.backend.dto.auth.RegisterRequestDto;
import org.plishka.backend.dto.auth.ResetPasswordRequestDto;
import org.plishka.backend.event.auth.EmailVerificationRequestedEvent;
import org.plishka.backend.event.auth.PasswordResetRequestedEvent;
import org.plishka.backend.exception.AuthenticationFailedException;
import org.plishka.backend.exception.EmailAlreadyExistsException;
import org.plishka.backend.exception.EmailNotVerifiedException;
import org.plishka.backend.exception.ForbiddenException;
import org.plishka.backend.exception.InvalidPasswordResetTokenException;
import org.plishka.backend.exception.InvalidVerificationTokenException;
import org.plishka.backend.exception.RefreshTokenDeviceMismatchException;
import org.plishka.backend.exception.RefreshTokenExpiredException;
import org.plishka.backend.exception.RefreshTokenNotFoundException;
import org.plishka.backend.monitoring.metrics.BusinessMetricsRecorder;
import org.plishka.backend.monitoring.transaction.TransactionalMetricsPublisher;
import org.plishka.backend.repository.user.EmailVerificationTokenRepository;
import org.plishka.backend.repository.user.PasswordResetTokenRepository;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.RoleRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.auth.AuthService;
import org.plishka.backend.service.auth.JwtService;
import org.plishka.backend.util.TokenGenerator;
import org.plishka.backend.util.TokenHashUtil;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service implementation.
 *
 * <p>Concurrency invariant for auth flows: whenever a method needs both a {@link User} row lock
 * and token-row locks, it must acquire them in this order: {@code User -> token rows}. Token rows
 * include refresh tokens, password-reset tokens, and email-verification tokens. This ordering must
 * be preserved across methods to avoid deadlocks between concurrent auth operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private static final String RESET_PASSWORD_PATH = "/#/reset-password?token=";
    private static final String VERIFY_TOKEN_PATH = "/auth/verify?token=";
    private static final String USERS_EMAIL_CONSTRAINT = "uk_users_email";
    private static final String AUTH_OPERATION_EMAIL_VERIFICATION = "email_verification";
    private static final String AUTH_OPERATION_LOGIN = "login";
    private static final String AUTH_OPERATION_PASSWORD_RESET = "password_reset";
    private static final String AUTH_OPERATION_PASSWORD_RESET_REQUEST = "password_reset_request";
    private static final String AUTH_OPERATION_REGISTER = "register";
    private static final String AUTH_OPERATION_VERIFICATION_RESEND = "verification_resend";
    private static final String OUTCOME_ACCEPTED = "accepted";
    private static final String OUTCOME_ALREADY_EXISTS = "already_exists";
    private static final String OUTCOME_EMAIL_NOT_VERIFIED = "email_not_verified";
    private static final String OUTCOME_ERROR = "error";
    private static final String OUTCOME_EXPIRED = "expired";
    private static final String OUTCOME_FORBIDDEN = "forbidden";
    private static final String OUTCOME_INVALID = "invalid";
    private static final String OUTCOME_SUCCESS = "success";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final BackendProperties backendProperties;
    private final FrontendProperties frontendProperties;
    private final Clock clock;
    private final BusinessMetricsRecorder businessMetricsRecorder;
    private final TransactionalMetricsPublisher transactionalMetricsPublisher;

    @Override
    @Transactional
    public void register(RegisterRequestDto request) {
        try {
            String normalizedEmail = UserInputNormalizer.normalizeEmail(request.email());
            User user = buildUser(request, normalizedEmail);

            saveUserOrThrowEmailAlreadyExists(user);

            String rawVerificationToken = TokenGenerator.generateEmailVerificationToken();
            emailVerificationTokenRepository.save(buildEmailVerificationToken(user, rawVerificationToken));

            applicationEventPublisher.publishEvent(
                    new EmailVerificationRequestedEvent(user.getEmail(), buildVerificationLink(rawVerificationToken))
            );

            recordAuthFlowAfterCompletion(AUTH_OPERATION_REGISTER, OUTCOME_SUCCESS);
            log.info("User registered: userId={}", user.getId());
        } catch (EmailAlreadyExistsException exception) {
            businessMetricsRecorder.recordAuthFlow(AUTH_OPERATION_REGISTER, OUTCOME_ALREADY_EXISTS);
            throw exception;
        } catch (RuntimeException exception) {
            businessMetricsRecorder.recordAuthFlow(AUTH_OPERATION_REGISTER, OUTCOME_ERROR);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void verifyEmail(String rawToken) {
        try {
            EmailVerificationToken verificationToken = findEmailVerificationTokenOrThrow(rawToken);

            User user = userRepository.findByIdForUpdate(verificationToken.getUser().getId())
                    .orElseThrow(() -> new InvalidVerificationTokenException("User not found"));

            if (user.isEmailVerified()) {
                emailVerificationTokenRepository.deleteAllByUser(user);
                recordAuthFlowAfterCompletion(AUTH_OPERATION_EMAIL_VERIFICATION, OUTCOME_SUCCESS);
                return;
            }

            // Lock the token row only after the User row is locked to keep the order: User -> token rows.
            EmailVerificationToken lockedVerificationToken = findEmailVerificationTokenForUpdateOrThrow(rawToken);
            if (lockedVerificationToken.getExpiresAt().isBefore(now())) {
                throw new InvalidVerificationTokenException("Verification token has expired");
            }

            user.setEmailVerified(true);
            emailVerificationTokenRepository.deleteAllByUser(user);

            recordAuthFlowAfterCompletion(AUTH_OPERATION_EMAIL_VERIFICATION, OUTCOME_SUCCESS);
            log.info("Email verified successfully, userId={}", user.getId());
        } catch (InvalidVerificationTokenException exception) {
            businessMetricsRecorder.recordAuthFlow(
                    AUTH_OPERATION_EMAIL_VERIFICATION,
                    tokenFailureOutcome(exception)
            );
            throw exception;
        } catch (RuntimeException exception) {
            businessMetricsRecorder.recordAuthFlow(AUTH_OPERATION_EMAIL_VERIFICATION, OUTCOME_ERROR);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        try {
            String normalizedEmail = UserInputNormalizer.normalizeEmail(email);

            userRepository.findByEmailForUpdate(normalizedEmail)
                    .filter(user -> !user.isEmailVerified())
                    .ifPresent(user -> {
                        emailVerificationTokenRepository.deleteAllByUser(user);
                        emailVerificationTokenRepository.flush();

                        String rawVerificationToken = TokenGenerator.generateEmailVerificationToken();
                        EmailVerificationToken emailVerificationToken =
                                buildEmailVerificationToken(user, rawVerificationToken);

                        emailVerificationTokenRepository.save(emailVerificationToken);

                        String verificationLink = buildVerificationLink(rawVerificationToken);
                        applicationEventPublisher.publishEvent(
                                new EmailVerificationRequestedEvent(user.getEmail(), verificationLink)
                        );

                        log.info("Verification email resent: userId={}", user.getId());
                    });
            recordAuthFlowAfterCompletion(AUTH_OPERATION_VERIFICATION_RESEND, OUTCOME_ACCEPTED);
        } catch (RuntimeException exception) {
            businessMetricsRecorder.recordAuthFlow(AUTH_OPERATION_VERIFICATION_RESEND, OUTCOME_ERROR);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        try {
            String normalizedEmail = UserInputNormalizer.normalizeEmail(email);

            userRepository.findByEmailForUpdate(normalizedEmail)
                    .filter(User::isEmailVerified)
                    .filter(user -> !user.isBanned())
                    .ifPresent(user -> {
                        passwordResetTokenRepository.deleteAllByUser(user);
                        passwordResetTokenRepository.flush();

                        String rawResetToken = TokenGenerator.generatePasswordResetToken();
                        PasswordResetToken passwordResetToken = buildPasswordResetToken(user, rawResetToken);

                        passwordResetTokenRepository.save(passwordResetToken);

                        applicationEventPublisher.publishEvent(
                                new PasswordResetRequestedEvent(
                                        user.getEmail(),
                                        buildResetPasswordLink(rawResetToken)
                                )
                        );

                        log.info("Password reset requested: userId={}", user.getId());
                    });
            recordAuthFlowAfterCompletion(AUTH_OPERATION_PASSWORD_RESET_REQUEST, OUTCOME_ACCEPTED);
        } catch (RuntimeException exception) {
            businessMetricsRecorder.recordAuthFlow(AUTH_OPERATION_PASSWORD_RESET_REQUEST, OUTCOME_ERROR);
            throw exception;
        }
    }

    @Override
    @Transactional(noRollbackFor = InvalidPasswordResetTokenException.class)
    public void resetPassword(ResetPasswordRequestDto request) {
        try {
            PasswordResetToken passwordResetToken = findPasswordResetTokenOrThrow(request.token());

            User lockedUser = userRepository.findByIdForUpdate(passwordResetToken.getUser().getId())
                    .orElseThrow(() -> new InvalidPasswordResetTokenException("User not found"));

            // Lock the token row only after the User row is locked to keep the order: User -> token rows.
            PasswordResetToken lockedPasswordResetToken = findPasswordResetTokenForUpdateOrThrow(request.token());
            if (lockedPasswordResetToken.getExpiresAt().isBefore(now())) {
                passwordResetTokenRepository.delete(lockedPasswordResetToken);
                throw new InvalidPasswordResetTokenException("Password reset token has expired");
            }

            lockedUser.setPasswordHash(passwordEncoder.encode(request.password()));
            refreshTokenRepository.deleteAllByUserId(lockedUser.getId());
            passwordResetTokenRepository.deleteAllByUser(lockedUser);

            recordAuthFlowAfterCompletion(AUTH_OPERATION_PASSWORD_RESET, OUTCOME_SUCCESS);
            log.info("Password reset successful: userId={}", lockedUser.getId());
        } catch (InvalidPasswordResetTokenException exception) {
            businessMetricsRecorder.recordAuthFlow(AUTH_OPERATION_PASSWORD_RESET, tokenFailureOutcome(exception));
            throw exception;
        } catch (RuntimeException exception) {
            businessMetricsRecorder.recordAuthFlow(AUTH_OPERATION_PASSWORD_RESET, OUTCOME_ERROR);
            throw exception;
        }
    }

    @Override
    @Transactional
    public AuthResponseDto login(LoginRequestDto request, String deviceId) {
        try {
            String normalizedEmail = UserInputNormalizer.normalizeEmail(request.email());

            User user = findUserByEmailOrThrow(normalizedEmail);
            validatePasswordOrThrow(request.password(), user);

            User lockedUser = findUserByIdForUpdateOrThrow(user.getId());
            if (!lockedUser.getPasswordHash().equals(user.getPasswordHash())) {
                throw new AuthenticationFailedException("Invalid email or password");
            }
            validateUserCanAuthenticateOrThrow(lockedUser);

            String normalizedDeviceId = normalizeDeviceId(deviceId);
            refreshTokenRepository.deleteAllByUserIdAndDeviceId(lockedUser.getId(), normalizedDeviceId);
            refreshTokenRepository.flush();

            final String accessToken = jwtService.generateAccessToken(lockedUser);
            final String rawRefreshToken = TokenGenerator.generateRefreshToken();

            persistRefreshToken(lockedUser, rawRefreshToken, normalizedDeviceId);

            recordAuthFlowAfterCompletion(AUTH_OPERATION_LOGIN, OUTCOME_SUCCESS);
            log.info("Login successful: userId={}", lockedUser.getId());

            return new AuthResponseDto(accessToken, rawRefreshToken);
        } catch (AuthenticationFailedException exception) {
            businessMetricsRecorder.recordAuthFlow(AUTH_OPERATION_LOGIN, OUTCOME_INVALID);
            throw exception;
        } catch (EmailNotVerifiedException exception) {
            businessMetricsRecorder.recordAuthFlow(AUTH_OPERATION_LOGIN, OUTCOME_EMAIL_NOT_VERIFIED);
            throw exception;
        } catch (ForbiddenException exception) {
            businessMetricsRecorder.recordAuthFlow(AUTH_OPERATION_LOGIN, OUTCOME_FORBIDDEN);
            throw exception;
        } catch (RuntimeException exception) {
            businessMetricsRecorder.recordAuthFlow(AUTH_OPERATION_LOGIN, OUTCOME_ERROR);
            throw exception;
        }
    }

    @Override
    @Transactional(noRollbackFor = RefreshTokenExpiredException.class)
    public AuthResponseDto refresh(String rawRefreshToken, String deviceId) {
        String normalizedDeviceId = normalizeDeviceId(deviceId);

        RefreshToken currentToken = findRefreshTokenOrThrow(rawRefreshToken);
        User lockedUser = findUserByIdForUpdateOrThrow(currentToken.getUser().getId());

        // Lock the token row only after the User row is locked to keep the order: User -> token rows.
        currentToken = findRefreshTokenForUpdateOrThrow(rawRefreshToken);
        validateRefreshTokenOrThrow(currentToken, normalizedDeviceId);
        validateUserCanAuthenticateOrThrow(lockedUser);

        refreshTokenRepository.deleteAllByUserIdAndDeviceId(lockedUser.getId(), normalizedDeviceId);
        refreshTokenRepository.flush();

        final String newAccessToken = jwtService.generateAccessToken(lockedUser);
        final String newRefreshToken = TokenGenerator.generateRefreshToken();

        persistRefreshToken(lockedUser, newRefreshToken, normalizedDeviceId);

        log.info("Token refresh successful: userId={}", lockedUser.getId());

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

        log.info("Logout current device: userId={}, deletedTokens={}", userId, deletedTokens);
    }

    private User buildUser(RegisterRequestDto request, String normalizedEmail) {
        return User.builder()
                .name(UserInputNormalizer.normalizeName(request.name()))
                .email(normalizedEmail)
                .phone(UserInputNormalizer.normalizePhone(request.phone()))
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(Set.of(
                        roleRepository.findByName(Role.RoleName.USER)
                                .orElseThrow(() -> new IllegalStateException("Role USER not found"))
                ))
                .isBanned(false)
                .isEmailVerified(false)
                .build();
    }

    private void saveUserOrThrowEmailAlreadyExists(User user) {
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isUsersEmailUniqueConstraintViolation(exception)) {
                throw new EmailAlreadyExistsException("User with this email already exists");
            }
            throw exception;
        }
    }

    private EmailVerificationToken buildEmailVerificationToken(User user, String rawToken) {
        return EmailVerificationToken.builder()
                .tokenHash(TokenHashUtil.sha256(rawToken))
                .user(user)
                .expiresAt(now().plus(backendProperties.auth().emailVerificationTokenTtl()))
                .build();
    }

    private PasswordResetToken buildPasswordResetToken(User user, String rawToken) {
        return PasswordResetToken.builder()
                .tokenHash(TokenHashUtil.sha256(rawToken))
                .user(user)
                .expiresAt(now().plus(backendProperties.auth().passwordResetTokenTtl()))
                .build();
    }

    private void persistRefreshToken(User user, String rawToken, String deviceId) {
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(TokenHashUtil.sha256(rawToken))
                .user(user)
                .deviceId(deviceId)
                .expiresAt(now().plus(backendProperties.refreshTokenExpiration()))
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

    private RefreshToken findRefreshTokenOrThrow(String rawRefreshToken) {
        String refreshTokenHash = TokenHashUtil.sha256(rawRefreshToken);

        return refreshTokenRepository.findByTokenHash(refreshTokenHash)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token not found"));
    }

    private EmailVerificationToken findEmailVerificationTokenOrThrow(String rawToken) {
        String verificationTokenHash = TokenHashUtil.sha256(rawToken);

        return emailVerificationTokenRepository.findByTokenHash(verificationTokenHash)
                .orElseThrow(() -> new InvalidVerificationTokenException("Verification token not found"));
    }

    private EmailVerificationToken findEmailVerificationTokenForUpdateOrThrow(String rawToken) {
        String verificationTokenHash = TokenHashUtil.sha256(rawToken);

        return emailVerificationTokenRepository.findByTokenHashForUpdate(verificationTokenHash)
                .orElseThrow(() -> new InvalidVerificationTokenException("Verification token not found"));
    }

    private PasswordResetToken findPasswordResetTokenOrThrow(String rawToken) {
        String passwordResetTokenHash = TokenHashUtil.sha256(rawToken);

        return passwordResetTokenRepository.findByTokenHash(passwordResetTokenHash)
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Password reset token not found"));
    }

    private PasswordResetToken findPasswordResetTokenForUpdateOrThrow(String rawToken) {
        String passwordResetTokenHash = TokenHashUtil.sha256(rawToken);

        return passwordResetTokenRepository.findByTokenHashForUpdate(passwordResetTokenHash)
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Password reset token not found"));
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

    private String tokenFailureOutcome(RuntimeException exception) {
        String message = exception.getMessage();
        return message != null && message.contains("expired") ? OUTCOME_EXPIRED : OUTCOME_INVALID;
    }

    private void recordAuthFlowAfterCompletion(String operation, String outcome) {
        transactionalMetricsPublisher.afterCompletionOrNow(
                () -> businessMetricsRecorder.recordAuthFlow(operation, outcome),
                () -> businessMetricsRecorder.recordAuthFlow(operation, OUTCOME_ERROR)
        );
    }

    private String buildVerificationLink(String rawVerificationToken) {
        return backendProperties.baseUrl() + VERIFY_TOKEN_PATH + rawVerificationToken;
    }

    private String buildResetPasswordLink(String rawResetToken) {
        return frontendProperties.baseUrl().replaceAll("/+$", "") + RESET_PASSWORD_PATH + rawResetToken;
    }

    private String normalizeDeviceId(String deviceId) {
        return UUID.fromString(deviceId.trim()).toString();
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
