package org.plishka.backend.controller.callback;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.callback.CallbackRequestCreateDto;
import org.plishka.backend.dto.callback.CallbackRequestDto;
import org.plishka.backend.service.callback.CallbackRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CallbackControllerTest extends BaseControllerTest {
    private static final Long USER_ID = 100L;
    private static final Long CALLBACK_REQUEST_ID = 40L;
    private static final int PAGE_NUMBER = 0;
    private static final int CALLBACK_REQUESTS_PAGE_SIZE = 10;
    private static final String USER_EMAIL = "serhii@example.com";
    private static final String USER_NAME = "Serhii";
    private static final String USER_PHONE = "+380501234567";
    private static final String CALLBACK_MESSAGE = "Please call me";
    private static final Instant CREATED_AT = Instant.parse("2026-05-29T10:15:30Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CallbackRequestService callbackRequestService;

    @Test
    void createCallbackRequest_ShouldReturnCreatedCallbackRequestAndStatus201() throws Exception {
        CallbackRequestCreateDto createRequest = callbackRequestCreateDto(CALLBACK_MESSAGE);
        when(callbackRequestService.createCallbackRequest(eq(USER_ID), any(CallbackRequestCreateDto.class)))
                .thenReturn(callbackRequest());

        mockMvc.perform(post("/callback")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.callbackRequestId").value(CALLBACK_REQUEST_ID))
                .andExpect(jsonPath("$.message").value(CALLBACK_MESSAGE));
    }

    @Test
    void getCallbackRequests_ShouldReturnPaginatedCallbackRequestsAndStatus200() throws Exception {
        PageResponse<CallbackRequestDto> response = new PageResponse<>(
                List.of(callbackRequest()),
                PAGE_NUMBER,
                CALLBACK_REQUESTS_PAGE_SIZE,
                1,
                1,
                true
        );

        when(callbackRequestService.getCallbackRequests(USER_ID, PAGE_NUMBER, CALLBACK_REQUESTS_PAGE_SIZE))
                .thenReturn(response);

        mockMvc.perform(get("/users/me/callback-requests")
                        .with(authenticatedUser(USER_ID, USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].callbackRequestId").value(CALLBACK_REQUEST_ID))
                .andExpect(jsonPath("$.content[0].message").value(CALLBACK_MESSAGE));
    }

    @Test
    void createCallbackRequest_ShouldReturn400_WhenMessageIsBlank() throws Exception {
        CallbackRequestCreateDto createRequest = callbackRequestCreateDto("");

        mockMvc.perform(post("/callback")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    private static CallbackRequestCreateDto callbackRequestCreateDto(String message) {
        return new CallbackRequestCreateDto(
                USER_NAME,
                USER_PHONE,
                message
        );
    }

    private static CallbackRequestDto callbackRequest() {
        return new CallbackRequestDto(
                CALLBACK_REQUEST_ID,
                USER_NAME,
                USER_PHONE,
                CALLBACK_MESSAGE,
                CREATED_AT
        );
    }
}
