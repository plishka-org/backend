package org.plishka.backend.service.auth.impl;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.user.EmailVerificationToken;
import org.plishka.backend.domain.user.PasswordResetToken;
import org.plishka.backend.domain.user.RefreshToken;
import org.plishka.backend.domain.user.Role;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.auth.AuthResponseDto;
import org.plishka.backend.dto.auth.LoginRequestDto;
import org.plishka.backend.dto.auth.RegisterRequestDto;
import org.plishka.backend.dto.auth.ResetPasswordRequestDto;
import org.plishka.backend.exception.EmailAlreadyExistsException;
import org.plishka.backend.exception.EmailNotVerifiedException;
import org.plishka.backend.exception.ForbiddenException;
import org.plishka.backend.exception.InvalidPasswordResetTokenException;
import org.plishka.backend.exception.InvalidVerificationTokenException;
import org.plishka.backend.exception.RefreshTokenExpiredException;
import org.plishka.backend.repository.user.EmailVerificationTokenRepository;
import org.plishka.backend.repository.user.PasswordResetTokenRepository;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.RoleRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.auth.AuthService;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.plishka.backend.testsupport.MySqlIntegrationTest;
import org.plishka.backend.util.TokenHashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceImplIntegrationTest extends MySqlIntegrationTest {
    private static final String DEVICE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String OLD_PASSWORD = "Password1";
    private static final String NEW_PASSWORD = "NewPassword1";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void register_ShouldCreateUnverifiedUserAndVerificationToken() {
        String email = uniqueEmail("register");

        authService.register(registerRequest("  Register User  ", "  " + email.toUpperCase() + "  "));

        User user = userRepository.findByEmail(email).orElseThrow();
        assertEquals("Register User", user.getName());
        assertFalse(user.isEmailVerified());
        assertFalse(user.isBanned());
        assertEquals(1, countEmailVerificationTokens(user.getId()));
    }

    @Test
    void register_ShouldRejectDuplicateEmail() {
        String email = uniqueEmail("duplicate");
        createUser(email, true, false);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(registerRequest("Duplicate User", email.toUpperCase()))
        );
    }

    @Test
    void verifyEmail_ShouldMarkEmailVerifiedAndDeleteVerificationTokens() {
        User user = createUser(uniqueEmail("verify"), false, false);
        String token = uniqueToken("verify");
        createEmailVerificationToken(user, token, Instant.now().plusSeconds(3600));

        authService.verifyEmail(token);

        User verifiedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(verifiedUser.isEmailVerified());
        assertEquals(0, countEmailVerificationTokens(user.getId()));
    }

    @Test
    void verifyEmail_ShouldRejectExpiredTokenAndLeaveUserUnverified() {
        User user = createUser(uniqueEmail("expired-verification"), false, false);
        String token = uniqueToken("expired-verification");
        createEmailVerificationToken(user, token, Instant.now().minusSeconds(60));

        assertThrows(InvalidVerificationTokenException.class, () -> authService.verifyEmail(token));

        User unchangedUser = userRepository.findById(user.getId()).orElseThrow();
        assertFalse(unchangedUser.isEmailVerified());
    }

    @Test
    void login_ShouldCreateAccessAndRefreshTokens_WhenUserIsActive() {
        User user = createUser(uniqueEmail("login"), true, false);

        AuthResponseDto response = authService.login(new LoginRequestDto(user.getEmail(), OLD_PASSWORD), DEVICE_ID);

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertTrue(refreshTokenRepository.findByTokenHash(TokenHashUtil.sha256(response.refreshToken())).isPresent());
    }

    @Test
    void login_ShouldRejectUnverifiedEmail() {
        User user = createUser(uniqueEmail("login-unverified"), false, false);

        assertThrows(
                EmailNotVerifiedException.class,
                () -> authService.login(new LoginRequestDto(user.getEmail(), OLD_PASSWORD), DEVICE_ID)
        );
    }

    @Test
    void login_ShouldRejectBannedUser() {
        User user = createUser(uniqueEmail("login-banned"), true, true);

        assertThrows(
                ForbiddenException.class,
                () -> authService.login(new LoginRequestDto(user.getEmail(), OLD_PASSWORD), DEVICE_ID)
        );
    }

    @Test
    void refresh_ShouldRotateRefreshTokenForDevice() {
        User user = createUser(uniqueEmail("refresh"), true, false);
        AuthResponseDto loginResponse = authService.login(new LoginRequestDto(user.getEmail(), OLD_PASSWORD), DEVICE_ID);

        AuthResponseDto refreshed = authService.refresh(loginResponse.refreshToken(), DEVICE_ID);

        assertNotEquals(loginResponse.refreshToken(), refreshed.refreshToken());
        assertFalse(refreshTokenRepository.findByTokenHash(TokenHashUtil.sha256(loginResponse.refreshToken()))
                .isPresent());
        assertTrue(refreshTokenRepository.findByTokenHash(TokenHashUtil.sha256(refreshed.refreshToken())).isPresent());
    }

    @Test
    void refresh_ShouldDeleteExpiredRefreshToken() {
        User user = createUser(uniqueEmail("expired-refresh"), true, false);
        String refreshToken = uniqueToken("expired-refresh");
        createRefreshToken(user, refreshToken, DEVICE_ID, Instant.now().minusSeconds(60));

        assertThrows(RefreshTokenExpiredException.class, () -> authService.refresh(refreshToken, DEVICE_ID));

        assertFalse(refreshTokenRepository.findByTokenHash(TokenHashUtil.sha256(refreshToken)).isPresent());
    }

    @Test
    void resetPassword_ShouldChangePasswordAndDeleteUserTokens() {
        User user = createUser(uniqueEmail("reset"), true, false);
        String refreshToken = uniqueToken("reset-refresh");
        String passwordResetToken = uniqueToken("reset-password");
        createRefreshToken(user, refreshToken, DEVICE_ID, Instant.now().plusSeconds(3600));
        createPasswordResetToken(user, passwordResetToken, Instant.now().plusSeconds(3600));

        authService.resetPassword(new ResetPasswordRequestDto(passwordResetToken, NEW_PASSWORD, NEW_PASSWORD));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, updatedUser.getPasswordHash()));
        assertEquals(0, countRefreshTokens(user.getId()));
        assertEquals(0, countPasswordResetTokens(user.getId()));
    }

    @Test
    void resetPassword_ShouldDeleteExpiredTokenAndKeepPassword() {
        User user = createUser(uniqueEmail("expired-reset"), true, false);
        String oldHash = user.getPasswordHash();
        String token = uniqueToken("expired-reset");
        createPasswordResetToken(user, token, Instant.now().minusSeconds(60));

        assertThrows(
                InvalidPasswordResetTokenException.class,
                () -> authService.resetPassword(new ResetPasswordRequestDto(token, NEW_PASSWORD,
                        NEW_PASSWORD))
        );

        User unchangedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(oldHash, unchangedUser.getPasswordHash());
        assertEquals(0, countPasswordResetTokens(user.getId()));
    }

    private User createUser(String email, boolean emailVerified, boolean banned) {
        User user = User.builder()
                .name("Integration User")
                .email(email)
                .passwordHash(passwordEncoder.encode(OLD_PASSWORD))
                .roles(Set.of(userRole()))
                .isEmailVerified(emailVerified)
                .isBanned(banned)
                .build();
        return userRepository.saveAndFlush(user);
    }

    private Role userRole() {
        return roleRepository.findByName(Role.RoleName.USER).orElseThrow();
    }

    private void createEmailVerificationToken(User user, String rawToken, Instant expiresAt) {
        emailVerificationTokenRepository.saveAndFlush(EmailVerificationToken.builder()
                .user(user)
                .tokenHash(TokenHashUtil.sha256(rawToken))
                .expiresAt(expiresAt)
                .build());
    }

    private void createPasswordResetToken(User user, String rawToken, Instant expiresAt) {
        passwordResetTokenRepository.saveAndFlush(PasswordResetToken.builder()
                .user(user)
                .tokenHash(TokenHashUtil.sha256(rawToken))
                .expiresAt(expiresAt)
                .build());
    }

    private void createRefreshToken(User user, String rawToken, String deviceId, Instant expiresAt) {
        refreshTokenRepository.saveAndFlush(RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHashUtil.sha256(rawToken))
                .deviceId(deviceId)
                .expiresAt(expiresAt)
                .build());
    }

    private RegisterRequestDto registerRequest(String name, String email) {
        return new RegisterRequestDto(name, email, null, OLD_PASSWORD, OLD_PASSWORD);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + System.nanoTime() + "@example.com";
    }

    private String uniqueToken(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

    private int countEmailVerificationTokens(Long userId) {
        return countRows("select count(*) from email_verification_tokens where user_id = ?", userId);
    }

    private int countPasswordResetTokens(Long userId) {
        return countRows("select count(*) from password_reset_tokens where user_id = ?", userId);
    }

    private int countRefreshTokens(Long userId) {
        return countRows("select count(*) from refresh_tokens where user_id = ?", userId);
    }

    private int countRows(String sql, Long userId) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Integer.class, userId));
    }
}
