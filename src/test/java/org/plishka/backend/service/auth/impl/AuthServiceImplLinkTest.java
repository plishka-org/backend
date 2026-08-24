package org.plishka.backend.service.auth.impl;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.config.properties.FrontendProperties;
import org.plishka.backend.monitoring.metrics.BusinessMetricsRecorder;
import org.plishka.backend.monitoring.transaction.TransactionalMetricsPublisher;
import org.plishka.backend.repository.user.EmailVerificationTokenRepository;
import org.plishka.backend.repository.user.PasswordResetTokenRepository;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.RoleRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.auth.JwtService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplLinkTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private BackendProperties backendProperties;

    @Mock
    private FrontendProperties frontendProperties;

    @Mock
    private Clock clock;

    @Mock
    private BusinessMetricsRecorder businessMetricsRecorder;

    @Mock
    private TransactionalMetricsPublisher transactionalMetricsPublisher;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void buildVerificationLink_ShouldUseRegisterRouteAndEncodeToken() {
        when(frontendProperties.baseUrl()).thenReturn("https://plishka-org.github.io/frontend///");

        assertEquals(
                "https://plishka-org.github.io/frontend/#/register?token=a%2Bb%2Fc%3Dd",
                ReflectionTestUtils.invokeMethod(authService, "buildVerificationLink", "a+b/c=d")
        );
    }

    @Test
    void buildResetPasswordLink_ShouldUseResetRouteAndEncodeToken() {
        when(frontendProperties.baseUrl()).thenReturn("https://plishka-org.github.io/frontend/");

        assertEquals(
                "https://plishka-org.github.io/frontend/#/reset-password?token=a%2Bb%2Fc%3Dd",
                ReflectionTestUtils.invokeMethod(authService, "buildResetPasswordLink", "a+b/c=d")
        );
    }
}
