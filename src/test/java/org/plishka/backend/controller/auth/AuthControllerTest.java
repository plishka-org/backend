package org.plishka.backend.controller.auth;

import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.auth.VerifyEmailChangeRequestDto;
import org.plishka.backend.service.auth.AuthService;
import org.plishka.backend.service.user.EmailChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest extends BaseControllerTest {
    private static final String EMAIL_CHANGE_TOKEN = "ABC1ABC1ABC1ABC1ABC1ABC1ABC1ABC1ABC1ABC1ABC";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private EmailChangeService emailChangeService;

    @Test
    void verifyEmail_ShouldRedirectToLoginWithVerifiedFlag() throws Exception {
        mockMvc.perform(get("/auth/verify")
                        .param("token", EMAIL_CHANGE_TOKEN))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:3000/#/login?verified=true"));

        verify(authService).verifyEmail(EMAIL_CHANGE_TOKEN);
    }

    @Test
    void verifyEmailChange_ShouldAcceptTokenFromPostBody() throws Exception {
        VerifyEmailChangeRequestDto request = new VerifyEmailChangeRequestDto(EMAIL_CHANGE_TOKEN);

        mockMvc.perform(post("/auth/verify-email-change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email changed successfully."));

        verify(emailChangeService).verifyEmailChange(EMAIL_CHANGE_TOKEN);
    }

    @Test
    void verifyEmailChange_ShouldNotAcceptTokenFromGetQuery() throws Exception {
        mockMvc.perform(get("/auth/verify-email-change")
                        .param("token", EMAIL_CHANGE_TOKEN))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(emailChangeService);
    }
}
