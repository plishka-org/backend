package org.plishka.backend.controller.admin;

import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.service.admin.catalog.category.AdminCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAccessSecurityTest extends BaseControllerTest {
    private static final String VALID_TOKEN = "valid-token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ADMIN_EMAIL = "admin@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCategoryService adminCategoryService;

    @Test
    void adminEndpoint_ShouldReturn401_WhenAnonymous() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_ShouldReturn403_WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/admin/categories")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_ShouldReturn403_WhenAdminIsBanned() throws Exception {
        mockJwtAuthentication(userPrincipal(1L, ADMIN_EMAIL, true, false, "ROLE_ADMIN"));

        mockMvc.perform(get("/admin/categories").header(AUTHORIZATION_HEADER, bearerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_ShouldReturn403_WhenAdminIsUnverified() throws Exception {
        mockJwtAuthentication(userPrincipal(1L, ADMIN_EMAIL, false, true, "ROLE_ADMIN"));

        mockMvc.perform(get("/admin/categories").header(AUTHORIZATION_HEADER, bearerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_ShouldReturn200_WhenAdminIsActive() throws Exception {
        mockJwtAuthentication(userPrincipal(1L, ADMIN_EMAIL, true, true, "ROLE_ADMIN"));
        when(adminCategoryService.getCategories(null)).thenReturn(List.of(new CategoryDto(1L, "Gazebos")));

        mockMvc.perform(get("/admin/categories")
                        .header(AUTHORIZATION_HEADER, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private void mockJwtAuthentication(AuthenticatedUserPrincipal principal) {
        Claims claims = mock(Claims.class);
        when(jwtService.parseClaims(VALID_TOKEN)).thenReturn(Optional.of(claims));
        when(jwtService.extractEmail(claims)).thenReturn(principal.getUsername());
        when(customUserDetailsService.loadUserByUsername(principal.getUsername())).thenReturn(principal);
    }

    private static String bearerToken() {
        return "Bearer " + VALID_TOKEN;
    }
}
