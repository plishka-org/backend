package org.plishka.backend.service.user.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.user.ChangePasswordRequestDto;
import org.plishka.backend.dto.user.DeleteAccountRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.user.EligibleUserProvider;
import org.plishka.backend.service.user.UserAccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountServiceImpl implements UserAccountService {
    private static final String CURRENT_PASSWORD_INCORRECT_MESSAGE = "Current password is incorrect";
    private static final String NEW_PASSWORD_SAME_AS_CURRENT_MESSAGE =
            "New password must be different from current password";

    private final EligibleUserProvider eligibleUserProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDto request) {
        User user = eligibleUserProvider.getEligibleUserForUpdateOrThrow(userId);
        validateCurrentPassword(request.currentPassword(), user);
        validateNewPasswordIsDifferent(request);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.deleteAllByUserId(user.getId());

        log.info("Password changed successfully: userId={}", user.getId());
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId, DeleteAccountRequestDto request) {
        User user = eligibleUserProvider.getEligibleUserForUpdateOrThrow(userId);
        validateCurrentPassword(request.currentPassword(), user);

        userRepository.deleteByIdDirect(user.getId());
        log.info("User account deleted: userId={}", userId);
    }

    private void validateCurrentPassword(String currentPassword, User user) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException(CURRENT_PASSWORD_INCORRECT_MESSAGE);
        }
    }

    private void validateNewPasswordIsDifferent(ChangePasswordRequestDto request) {
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BadRequestException(NEW_PASSWORD_SAME_AS_CURRENT_MESSAGE);
        }
    }
}
