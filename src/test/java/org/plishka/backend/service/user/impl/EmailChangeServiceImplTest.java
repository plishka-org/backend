package org.plishka.backend.service.user.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.config.properties.FrontendProperties;
import org.plishka.backend.domain.user.EmailChangeToken;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.exception.EmailAlreadyExistsException;
import org.plishka.backend.exception.InvalidVerificationTokenException;
import org.plishka.backend.repository.user.EmailChangeTokenRepository;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.util.TokenHashUtil;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailChangeServiceImplTest {
    private static final Long USER_ID = 100L;
    private static final String CURRENT_EMAIL = "current@example.com";
    private static final String NEW_EMAIL = "new@example.com";
    private static final String RAW_TOKEN = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String TOKEN_HASH = TokenHashUtil.sha256(RAW_TOKEN);
    private static final Instant NOW = Instant.parse("2026-06-06T08:00:00Z");

    @Mock
    private EmailChangeTokenRepository emailChangeTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private BackendProperties backendProperties;

    @Mock
    private FrontendProperties frontendProperties;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private EmailChangeServiceImpl emailChangeService;

    @BeforeEach
    void setUp() {
        emailChangeService = new EmailChangeServiceImpl(
                emailChangeTokenRepository,
                userRepository,
                refreshTokenRepository,
                applicationEventPublisher,
                backendProperties,
                frontendProperties,
                clock
        );
    }

    @Test
    void verifyEmailChange_ShouldRejectAndDeleteToken_WhenUserIsBanned() {
        User tokenUser = user(true, false);
        User lockedUser = user(true, true);
        EmailChangeToken token = token(tokenUser);

        when(emailChangeTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(token));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(lockedUser));
        when(emailChangeTokenRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(token));

        assertThrows(
                InvalidVerificationTokenException.class,
                () -> emailChangeService.verifyEmailChange(RAW_TOKEN)
        );

        verify(emailChangeTokenRepository).delete(token);
        verify(refreshTokenRepository, never()).deleteAllByUserId(USER_ID);
        verify(userRepository, never()).flush();
    }

    @Test
    void verifyEmailChange_ShouldInvalidateRefreshTokens_WhenEmailIsChanged() {
        User lockedUser = user(true, false);
        EmailChangeToken token = token(lockedUser);

        when(emailChangeTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(token));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(lockedUser));
        when(emailChangeTokenRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(token));
        when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(false);

        emailChangeService.verifyEmailChange(RAW_TOKEN);

        assertEquals(NEW_EMAIL, lockedUser.getEmail());
        verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
        verify(userRepository).flush();
        verify(emailChangeTokenRepository).deleteAllByUser(lockedUser);
    }

    @Test
    void initiateEmailChange_ShouldThrowGenericMessage_WhenEmailAlreadyExists() {
        User user = user(true, false);
        when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(true);

        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> emailChangeService.initiateEmailChange(user, NEW_EMAIL)
        );

        assertEquals("Email already exists", exception.getMessage());
        assertFalse(exception.getMessage().contains(NEW_EMAIL));
        verify(emailChangeTokenRepository, never()).saveAndFlush(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    private static User user(boolean emailVerified, boolean banned) {
        return User.builder()
                .id(USER_ID)
                .email(CURRENT_EMAIL)
                .isEmailVerified(emailVerified)
                .isBanned(banned)
                .build();
    }

    private static EmailChangeToken token(User user) {
        return EmailChangeToken.builder()
                .user(user)
                .tokenHash(TOKEN_HASH)
                .newEmail(NEW_EMAIL)
                .expiresAt(NOW.plusSeconds(3600))
                .build();
    }
}
