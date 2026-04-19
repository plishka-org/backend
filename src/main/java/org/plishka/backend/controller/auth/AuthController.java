package org.plishka.backend.controller.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.auth.AuthResponseDto;
import org.plishka.backend.dto.auth.LoginRequestDto;
import org.plishka.backend.dto.auth.RefreshTokenRequestDto;
import org.plishka.backend.dto.auth.RegisterRequestDto;
import org.plishka.backend.dto.auth.ResendVerificationEmailRequestDto;
import org.plishka.backend.dto.common.MessageResponseDto;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.service.auth.AuthService;
import org.plishka.backend.validation.ValidDeviceId;
import org.plishka.backend.validation.ValidEmailActionToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String DEVICE_ID_HEADER = "Device-Id";
    private final AuthService authService;

    @PostMapping("/register")
    public MessageResponseDto register(@Valid @RequestBody RegisterRequestDto requestDto) {
        authService.register(requestDto);
        return new MessageResponseDto("Registration successful. Please check your email to verify your account.");
    }

    @GetMapping("/verify")
    public MessageResponseDto verifyEmail(@RequestParam @ValidEmailActionToken String token) {
        authService.verifyEmail(token);
        return new MessageResponseDto("Email verified successfully.");
    }

    @PostMapping("/resend-verification")
    public MessageResponseDto resendVerificationEmail(
            @Valid @RequestBody ResendVerificationEmailRequestDto requestDto
    ) {
        authService.resendVerificationEmail(requestDto.email());
        return new MessageResponseDto(
                "If an unverified account exists, a new verification email has been sent."
        );
    }

    @PostMapping("/login")
    public AuthResponseDto login(
            @Valid @RequestBody LoginRequestDto requestDto,
            @RequestHeader(DEVICE_ID_HEADER) @ValidDeviceId String deviceId
    ) {
        return authService.login(requestDto, deviceId);
    }

    @PostMapping("/refresh")
    public AuthResponseDto refresh(
            @Valid @RequestBody RefreshTokenRequestDto requestDto,
            @RequestHeader(DEVICE_ID_HEADER) @ValidDeviceId String deviceId
    ) {
        return authService.refresh(requestDto.refreshToken(), deviceId);
    }

    @PostMapping("/logout")
    public MessageResponseDto logout(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestHeader(DEVICE_ID_HEADER) @ValidDeviceId String deviceId
    ) {
        authService.logoutCurrentDevice(principal.getUserId(), deviceId);
        return new MessageResponseDto("Logged out successfully.");
    }
}
