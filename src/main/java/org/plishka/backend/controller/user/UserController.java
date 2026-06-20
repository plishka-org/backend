package org.plishka.backend.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.common.MessageResponseDto;
import org.plishka.backend.dto.user.ChangeEmailRequestDto;
import org.plishka.backend.dto.user.ChangePasswordRequestDto;
import org.plishka.backend.dto.user.DeleteAccountRequestDto;
import org.plishka.backend.dto.user.UserProfileDto;
import org.plishka.backend.dto.user.UserProfileUpdateRequestDto;
import org.plishka.backend.dto.user.UserProfileUpdateResponseDto;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.service.user.UserAccountService;
import org.plishka.backend.service.user.UserProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {
    private final UserProfileService userProfileService;
    private final UserAccountService userAccountService;

    @GetMapping
    public UserProfileDto getCurrentUser(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return userProfileService.getCurrentUser(principal.getUserId());
    }

    @PutMapping
    public UserProfileUpdateResponseDto updateCurrentUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody UserProfileUpdateRequestDto request
    ) {
        return userProfileService.updateCurrentUser(principal.getUserId(), request);
    }

    @PutMapping("/email")
    public MessageResponseDto requestEmailChange(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody ChangeEmailRequestDto request
    ) {
        userProfileService.requestEmailChange(principal.getUserId(), request);
        return new MessageResponseDto("Email change verification has been sent.");
    }

    @PutMapping("/password")
    public MessageResponseDto changePassword(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequestDto request
    ) {
        userAccountService.changePassword(principal.getUserId(), request);
        return new MessageResponseDto("Password changed successfully.");
    }

    @DeleteMapping
    public MessageResponseDto deleteAccount(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody DeleteAccountRequestDto request
    ) {
        userAccountService.deleteAccount(principal.getUserId(), request);
        return new MessageResponseDto("Account deleted successfully.");
    }
}
