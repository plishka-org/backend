package org.plishka.backend.controller.admin;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.callback.CallbackRequestDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.service.admin.callback.AdminCallbackRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCallbackRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCallbackRequestControllerTest extends BaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCallbackRequestService adminCallbackRequestService;

    @Test
    void getCallbackRequests_ShouldReturnPagedRequests() throws Exception {
        when(adminCallbackRequestService.getCallbackRequests(any(), eq(0), eq(10))).thenReturn(new PageResponse<>(
                List.of(new CallbackRequestDto(
                        1L,
                        "Olena Shevchenko",
                        "+380501234567",
                        "Please call me",
                        Instant.parse("2026-07-01T12:00:00Z")
                )),
                0,
                10,
                1,
                1,
                true
        ));

        mockMvc.perform(get("/admin/callback-requests").param("search", "050"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].callbackRequestId").value(1))
                .andExpect(jsonPath("$.content[0].phone").value("+380501234567"));
    }
}
