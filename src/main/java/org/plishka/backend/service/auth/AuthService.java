package org.plishka.backend.service.auth;

import org.plishka.backend.dto.auth.AuthResponseDto;
import org.plishka.backend.dto.auth.LoginRequestDto;
import org.plishka.backend.dto.auth.RegisterRequestDto;

public interface AuthService {
    void register(RegisterRequestDto requestDto);

    void verifyEmail(String token);

    void resendVerificationEmail(String email);

    AuthResponseDto login(LoginRequestDto requestDto, String deviceId);

    AuthResponseDto refresh(String refreshToken, String deviceId);

    void logoutCurrentDevice(Long userId, String deviceId);
}
