package org.plishka.backend.service.user.impl;

import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.config.properties.FrontendProperties;
import org.plishka.backend.domain.user.EmailChangeToken;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.event.auth.EmailChangeRequestedEvent;
import org.plishka.backend.event.auth.EmailChangedEvent;
import org.plishka.backend.exception.EmailAlreadyExistsException;
import org.plishka.backend.exception.InvalidVerificationTokenException;
import org.plishka.backend.repository.user.EmailChangeTokenRepository;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.user.EmailChangeService;
import org.plishka.backend.util.TokenGenerator;
import org.plishka.backend.util.TokenHashUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailChangeServiceImpl implements EmailChangeService {
    private static final String EMAIL_CHANGE_VERIFICATION_PATH = "/#/verify-email-change?token=";
    private static final String USERS_EMAIL_CONSTRAINT = "uk_users_email";
    private static final String USER_WITH_EMAIL_ALREADY_EXISTS_MESSAGE = "User with email '%s' already exists";

    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final BackendProperties backendProperties;
    private final FrontendProperties frontendProperties;
    private final Clock clock;

    @Override
    @Transactional
    public void initiateEmailChange(User user, String newEmail) {
        validateEmailIsAvailable(newEmail);
        Instant now = now();

        emailChangeTokenRepository.deleteAllByUser(user);
        emailChangeTokenRepository.flush();

        String rawToken = TokenGenerator.generateEmailVerificationToken();
        EmailChangeToken token = buildEmailChangeToken(user, newEmail, rawToken, now);

        try {
            emailChangeTokenRepository.saveAndFlush(token);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailConstraintViolation(exception)) {
                throw new EmailAlreadyExistsException(USER_WITH_EMAIL_ALREADY_EXISTS_MESSAGE.formatted(newEmail));
            }
            throw exception;
        }

        applicationEventPublisher.publishEvent(new EmailChangeRequestedEvent(
                newEmail,
                buildEmailChangeVerificationLink(rawToken)
        ));
    }

    @Override
    @Transactional(noRollbackFor = InvalidVerificationTokenException.class)
    public void verifyEmailChange(String rawToken) {
        String tokenHash = TokenHashUtil.sha256(rawToken);
        EmailChangeToken token = findTokenOrThrow(tokenHash);
        User user = findUserForUpdateOrThrow(token.getUser().getId());
        EmailChangeToken lockedToken = findTokenForUpdateOrThrow(tokenHash);
        validateUserCanVerifyEmailChange(user, lockedToken);

        if (lockedToken.getExpiresAt().isBefore(now())) {
            emailChangeTokenRepository.delete(lockedToken);
            throw new InvalidVerificationTokenException("Email change token has expired");
        }

        final String oldEmail = user.getEmail();
        String newEmail = lockedToken.getNewEmail();
        validateEmailIsAvailable(newEmail);

        user.setEmail(newEmail);
        refreshTokenRepository.deleteAllByUserId(user.getId());
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (isEmailConstraintViolation(exception)) {
                throw new EmailAlreadyExistsException(USER_WITH_EMAIL_ALREADY_EXISTS_MESSAGE.formatted(newEmail));
            }
            throw exception;
        }
        emailChangeTokenRepository.deleteAllByUser(user);
        applicationEventPublisher.publishEvent(new EmailChangedEvent(oldEmail, newEmail));

        log.info("Email change verified successfully: userId={}, newEmail={}", user.getId(), newEmail);
    }

    private EmailChangeToken buildEmailChangeToken(User user, String newEmail, String rawToken, Instant now) {
        return EmailChangeToken.builder()
                .user(user)
                .newEmail(newEmail)
                .tokenHash(TokenHashUtil.sha256(rawToken))
                .expiresAt(now.plus(backendProperties.auth().emailVerificationTokenTtl()))
                .build();
    }

    private User findUserForUpdateOrThrow(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new InvalidVerificationTokenException("User not found"));
    }

    private EmailChangeToken findTokenOrThrow(String tokenHash) {
        return emailChangeTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidVerificationTokenException("Email change token not found"));
    }

    private EmailChangeToken findTokenForUpdateOrThrow(String tokenHash) {
        return emailChangeTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new InvalidVerificationTokenException("Email change token not found"));
    }

    private void validateEmailIsAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(USER_WITH_EMAIL_ALREADY_EXISTS_MESSAGE.formatted(email));
        }
    }

    private boolean isEmailConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && isEmailConstraint(violation.getConstraintName())) {
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

    private boolean isEmailConstraint(String constraintName) {
        return USERS_EMAIL_CONSTRAINT.equalsIgnoreCase(constraintName);
    }

    private void validateUserCanVerifyEmailChange(User user, EmailChangeToken token) {
        if (user.isEmailVerified() && !user.isBanned()) {
            return;
        }

        emailChangeTokenRepository.delete(token);
        throw new InvalidVerificationTokenException("Email change token is no longer valid");
    }

    private String buildEmailChangeVerificationLink(String rawToken) {
        return buildFrontendUrl(EMAIL_CHANGE_VERIFICATION_PATH) + rawToken;
    }

    private String buildFrontendUrl(String path) {
        return frontendProperties.baseUrl().replaceAll("/+$", "") + path;
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
