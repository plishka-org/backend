package org.plishka.backend.service.user;

import org.plishka.backend.dto.user.ChangeEmailRequestDto;
import org.plishka.backend.dto.user.UserProfileDto;
import org.plishka.backend.dto.user.UserProfileUpdateRequestDto;
import org.plishka.backend.dto.user.UserProfileUpdateResponseDto;

public interface UserProfileService {
    UserProfileDto getCurrentUser(Long userId);

    UserProfileUpdateResponseDto updateCurrentUser(Long userId, UserProfileUpdateRequestDto request);

    void requestEmailChange(Long userId, ChangeEmailRequestDto request);
}
