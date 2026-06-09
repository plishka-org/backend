package org.plishka.backend.controller.user;

import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.user.ChangeEmailRequestDto;
import org.plishka.backend.dto.user.ChangePasswordRequestDto;
import org.plishka.backend.dto.user.DeleteAccountRequestDto;
import org.plishka.backend.dto.user.UserProfileDto;
import org.plishka.backend.dto.user.UserProfileUpdateRequestDto;
import org.plishka.backend.dto.user.UserProfileUpdateResponseDto;
import org.plishka.backend.service.user.UserAccountService;
import org.plishka.backend.service.user.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest extends BaseControllerTest {
    private static final Long USER_ID = 100L;
    private static final String USER_NAME = "Serhii";
    private static final String USER_EMAIL = "serhii@example.com";
    private static final String USER_PHONE = "+380501234567";
    private static final String NEW_USER_EMAIL = "new@example.com";
    private static final String CURRENT_PASSWORD = "OldPassword123";
    private static final String NEW_PASSWORD = "NewPassword123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserAccountService userAccountService;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    void getCurrentUser_ShouldReturnCurrentUserAndStatus200() throws Exception {
        when(userProfileService.getCurrentUser(USER_ID)).thenReturn(userProfile());

        mockMvc.perform(get("/users/me")
                        .with(currentUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.name").value(USER_NAME))
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.phone").value(USER_PHONE));
    }

    @Test
    void updateCurrentUser_ShouldReturnUpdatedProfile() throws Exception {
        UserProfileUpdateRequestDto updateRequest = new UserProfileUpdateRequestDto(
                USER_NAME,
                USER_PHONE
        );
        UserProfileUpdateResponseDto response = new UserProfileUpdateResponseDto(
                USER_ID,
                USER_NAME,
                USER_EMAIL,
                USER_PHONE
        );

        when(userProfileService.updateCurrentUser(eq(USER_ID), any(UserProfileUpdateRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(put("/users/me")
                        .with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.phone").value(USER_PHONE));
    }

    @Test
    void updateCurrentUser_ShouldReturn400_WhenNameIsInvalid() throws Exception {
        UserProfileUpdateRequestDto updateRequest = new UserProfileUpdateRequestDto(
                "",
                USER_PHONE
        );

        mockMvc.perform(put("/users/me")
                        .with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestEmailChange_ShouldReturnMessageAndStatus200() throws Exception {
        ChangeEmailRequestDto request = new ChangeEmailRequestDto(
                NEW_USER_EMAIL,
                CURRENT_PASSWORD
        );

        mockMvc.perform(put("/users/me/email")
                        .with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email change verification has been sent."));

        verify(userProfileService).requestEmailChange(eq(USER_ID), any(ChangeEmailRequestDto.class));
    }

    @Test
    void changePassword_ShouldReturnMessageAndStatus200() throws Exception {
        ChangePasswordRequestDto changePasswordRequest = new ChangePasswordRequestDto(
                CURRENT_PASSWORD,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        mockMvc.perform(put("/users/me/password")
                        .with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully."));

        verify(userAccountService).changePassword(eq(USER_ID), any(ChangePasswordRequestDto.class));
    }

    @Test
    void changePassword_ShouldReturn400_WhenPasswordsDoNotMatch() throws Exception {
        ChangePasswordRequestDto changePasswordRequest = new ChangePasswordRequestDto(
                CURRENT_PASSWORD,
                NEW_PASSWORD,
                "DifferentPassword123"
        );

        mockMvc.perform(put("/users/me/password")
                        .with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAccount_ShouldReturnMessageAndStatus200() throws Exception {
        DeleteAccountRequestDto deleteAccountRequest = new DeleteAccountRequestDto(CURRENT_PASSWORD);

        mockMvc.perform(delete("/users/me")
                        .with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteAccountRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deleted successfully."));

        verify(userAccountService).deleteAccount(eq(USER_ID), any(DeleteAccountRequestDto.class));
    }

    private static RequestPostProcessor currentUser() {
        return authenticatedUser(USER_ID, USER_EMAIL);
    }

    private static UserProfileDto userProfile() {
        return new UserProfileDto(USER_ID, USER_NAME, USER_EMAIL, USER_PHONE);
    }
}
