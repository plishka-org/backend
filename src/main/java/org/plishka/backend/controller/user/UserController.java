package org.plishka.backend.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserProfileService userProfileService;
    private final UserAccountService userAccountService;

    @Operation(
            operationId = "getCurrentUser",
            summary = "Get current user profile",
            description = "Requires an active user: authenticated, email verified, and not banned."
    )
    @GetMapping
    public UserProfileDto getCurrentUser(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return userProfileService.getCurrentUser(principal.getUserId());
    }

    @Operation(
            operationId = "updateCurrentUser",
            summary = "Update current user profile",
            description = "Requires an active user: authenticated, email verified, and not banned."
    )
    @PutMapping
    public UserProfileUpdateResponseDto updateCurrentUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody UserProfileUpdateRequestDto request
    ) {
        return userProfileService.updateCurrentUser(principal.getUserId(), request);
    }

    @Operation(
            operationId = "requestEmailChange",
            summary = "Request email change",
            description = "Requires an active user: authenticated, email verified, and not banned. "
                    + "A verification email is sent to the requested new email address."
    )
    @PutMapping("/email")
    public MessageResponseDto requestEmailChange(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody ChangeEmailRequestDto request
    ) {
        userProfileService.requestEmailChange(principal.getUserId(), request);
        return new MessageResponseDto("Email change verification has been sent.");
    }

    @Operation(
            operationId = "changePassword",
            summary = "Change password",
            description = "Requires an active user: authenticated, email verified, and not banned."
    )
    @PutMapping("/password")
    public MessageResponseDto changePassword(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequestDto request
    ) {
        userAccountService.changePassword(principal.getUserId(), request);
        return new MessageResponseDto("Password changed successfully.");
    }

    @Operation(
            operationId = "deleteAccount",
            summary = "Delete current user account",
            description = "Requires an active user: authenticated, email verified, and not banned."
    )
    @DeleteMapping
    public MessageResponseDto deleteAccount(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody DeleteAccountRequestDto request
    ) {
        userAccountService.deleteAccount(principal.getUserId(), request);
        return new MessageResponseDto("Account deleted successfully.");
    }
}
