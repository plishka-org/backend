package org.plishka.backend.service.admin.home;

import org.junit.jupiter.api.Test;
import org.plishka.backend.dto.admin.home.AdminHomePageContentRequestDto;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminHomePageServiceImplIntegrationTest {
    private static final long SINGLETON_ID = 1L;

    @Autowired
    private AdminHomePageService adminHomePageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void homePageFlow_ShouldUpdateContent() {
        adminHomePageService.updateHomePageContent(
                new AdminHomePageContentRequestDto("Updated home title", "Updated home description")
        );

        assertEquals("Updated home title", findHomePageTitle());
        assertEquals("Updated home description", findHomePageDescription());
    }

    private String findHomePageTitle() {
        return jdbcTemplate.queryForObject("select title from home_page_content where id = ?", String.class, SINGLETON_ID);
    }

    private String findHomePageDescription() {
        return jdbcTemplate.queryForObject(
                "select description from home_page_content where id = ?",
                String.class,
                SINGLETON_ID
        );
    }
}
