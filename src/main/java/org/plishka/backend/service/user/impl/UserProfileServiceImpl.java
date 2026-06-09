package org.plishka.backend.service.user.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.user.ChangeEmailRequestDto;
import org.plishka.backend.dto.user.UserProfileDto;
import org.plishka.backend.dto.user.UserProfileUpdateRequestDto;
import org.plishka.backend.dto.user.UserProfileUpdateResponseDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.mapper.user.UserProfileMapper;
import org.plishka.backend.service.user.EligibleUserProvider;
import org.plishka.backend.service.user.EmailChangeService;
import org.plishka.backend.service.user.UserProfileService;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {
    private final EligibleUserProvider eligibleUserProvider;
    private final EmailChangeService emailChangeService;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUser(Long userId) {
        return userProfileMapper.toDto(eligibleUserProvider.getEligibleUserOrThrow(userId));
    }

    @Override
    @Transactional
    public UserProfileUpdateResponseDto updateCurrentUser(Long userId, UserProfileUpdateRequestDto request) {
        User user = eligibleUserProvider.getEligibleUserForUpdateOrThrow(userId);

        user.setName(UserInputNormalizer.normalizeName(request.name()));
        user.setPhone(UserInputNormalizer.normalizePhone(request.phone()));

        log.info("User profile updated: userId={}", user.getId());

        return userProfileMapper.toUpdateResponse(user);
    }

    @Override
    @Transactional
    public void requestEmailChange(Long userId, ChangeEmailRequestDto request) {
        User user = eligibleUserProvider.getEligibleUserForUpdateOrThrow(userId);
        validateCurrentPassword(request.currentPassword(), user);

        String normalizedEmail = UserInputNormalizer.normalizeEmail(request.newEmail());
        if (user.getEmail().equals(normalizedEmail)) {
            throw new BadRequestException("New email must be different from current email");
        }

        emailChangeService.initiateEmailChange(user, normalizedEmail);
        log.info("Email change requested: userId={}", user.getId());
    }

    private void validateCurrentPassword(String currentPassword, User user) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
    }
}
