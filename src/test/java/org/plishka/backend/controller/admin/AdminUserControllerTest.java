package org.plishka.backend.controller.admin;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.admin.common.BulkOperationResultDto;
import org.plishka.backend.dto.admin.user.AdminUserDto;
import org.plishka.backend.dto.admin.user.AdminUserSearchRequestDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.exception.ForbiddenException;
import org.plishka.backend.service.admin.user.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest extends BaseControllerTest {
    private static final long USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    void getUsers_ShouldReturnPaginatedUsers() throws Exception {
        PageResponse<AdminUserDto> response = new PageResponse<>(List.of(user()), 0, 20, 1, 1, true);
        when(adminUserService.getUsers(any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/admin/users")
                        .with(authenticatedAdmin())
                        .param("search", "serhii")
                        .param("sort", "createdAt,desc")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(USER_ID))
                .andExpect(jsonPath("$.content[0].isBanned").value(false))
                .andExpect(jsonPath("$.content[0].numberOfOrders").value(7));

        ArgumentCaptor<AdminUserSearchRequestDto> requestCaptor =
                ArgumentCaptor.forClass(AdminUserSearchRequestDto.class);
        verify(adminUserService).getUsers(requestCaptor.capture(), eq(2), eq(10));
        AdminUserSearchRequestDto request = requestCaptor.getValue();
        assertEquals("serhii", request.search());
        assertEquals("createdAt,desc", request.sort());
    }

    @Test
    void getBannedUsers_ShouldReturnOnlyBannedUsers() throws Exception {
        PageResponse<AdminUserDto> response = new PageResponse<>(
                List.of(new AdminUserDto(USER_ID, "Serhii", "serhii@example.com", "+380501234567", true, 1)),
                0,
                20,
                1,
                1,
                true
        );
        when(adminUserService.getBannedUsers(any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/admin/users/banned")
                        .with(authenticatedAdmin())
                        .param("search", "blocked")
                        .param("sort", "id,desc")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].isBanned").value(true));

        ArgumentCaptor<AdminUserSearchRequestDto> requestCaptor =
                ArgumentCaptor.forClass(AdminUserSearchRequestDto.class);
        verify(adminUserService).getBannedUsers(requestCaptor.capture(), eq(1), eq(5));
        AdminUserSearchRequestDto request = requestCaptor.getValue();
        assertEquals("blocked", request.search());
        assertEquals("id,desc", request.sort());
    }

    @Test
    void banUser_ShouldPassCurrentAdminIdToService() throws Exception {
        when(adminUserService.banUser(ADMIN_ID, USER_ID)).thenReturn(user());

        mockMvc.perform(put("/admin/users/{id}/ban", USER_ID)
                        .with(authenticatedAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID));
    }

    @Test
    void unbanUser_ShouldReturnUser() throws Exception {
        when(adminUserService.unbanUser(USER_ID)).thenReturn(user());

        mockMvc.perform(put("/admin/users/{id}/unban", USER_ID)
                        .with(authenticatedAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID));
    }

    @Test
    void banUser_ShouldReturn403_WhenServiceRejectsRule() throws Exception {
        when(adminUserService.banUser(eq(ADMIN_ID), eq(USER_ID)))
                .thenThrow(new ForbiddenException("Admin cannot ban another admin"));

        mockMvc.perform(put("/admin/users/{id}/ban", USER_ID)
                        .with(authenticatedAdmin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void bulkBan_ShouldReturn400_WhenIdsAreMissing() throws Exception {
        mockMvc.perform(post("/admin/users/ban/bulk")
                        .with(authenticatedAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulkBan_ShouldPassCurrentAdminIdToService() throws Exception {
        when(adminUserService.banUsers(eq(ADMIN_ID), any())).thenReturn(new BulkOperationResultDto(2));

        mockMvc.perform(post("/admin/users/ban/bulk")
                        .with(authenticatedAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[2,3]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedCount").value(2));
    }

    @Test
    void bulkUnban_ShouldReturnAffectedCount() throws Exception {
        when(adminUserService.unbanUsers(any())).thenReturn(new BulkOperationResultDto(2));

        mockMvc.perform(post("/admin/users/unban/bulk")
                        .with(authenticatedAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[2,3]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedCount").value(2));
    }

    private static AdminUserDto user() {
        return new AdminUserDto(USER_ID, "Serhii", "serhii@example.com", "+380501234567", false, 7);
    }
}
