package org.plishka.backend.controller.error;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiErrorControllerTest extends BaseControllerTest {
    private static final String UNKNOWN_ENDPOINT = "/svjdsLhvs13xlkx";
    private static final String PROTECTED_ENDPOINT = "/users/me";
    private static final String ORIGINAL_ERROR_PATH = "/api/missing";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownAnonymousEndpoint_ShouldReturn404ErrorResponse() throws Exception {
        mockMvc.perform(get(UNKNOWN_ENDPOINT))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Endpoint not found"))
                .andExpect(jsonPath("$.path").value(UNKNOWN_ENDPOINT));
    }

    @Test
    void protectedAnonymousEndpoint_ShouldReturn401ErrorResponse() throws Exception {
        mockMvc.perform(get(PROTECTED_ENDPOINT))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid or missing credentials"))
                .andExpect(jsonPath("$.path").value(PROTECTED_ENDPOINT));
    }

    @Test
    void errorEndpoint_ShouldReturnConfiguredErrorResponse() throws Exception {
        mockMvc.perform(get("/error")
                        .with(request -> {
                            request.setDispatcherType(DispatcherType.ERROR);
                            return request;
                        })
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, ORIGINAL_ERROR_PATH))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Endpoint not found"))
                .andExpect(jsonPath("$.path").value(ORIGINAL_ERROR_PATH));
    }

    @Test
    void directErrorEndpointRequest_ShouldReturn404ErrorResponse() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Endpoint not found"))
                .andExpect(jsonPath("$.path").value("/error"));
    }
}
