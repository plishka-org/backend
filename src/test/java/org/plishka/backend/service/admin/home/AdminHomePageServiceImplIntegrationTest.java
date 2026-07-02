package org.plishka.backend.service.admin.home;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.dto.admin.home.AdminHomePageAdvantageDto;
import org.plishka.backend.dto.admin.home.AdminHomePageAdvantageRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageContentRequestDto;
import org.plishka.backend.dto.admin.home.HomeAdvantagesOrderRequestDto;
import org.plishka.backend.repository.home.HomePageAdvantageRepository;
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
    private static final String ICON_OLD_KEY =
            "about/1/images/2026/05/550e8400-e29b-41d4-a716-446655440000.png";
    private static final String ICON_NEW_KEY =
            "about/1/images/2026/05/550e8400-e29b-41d4-a716-446655440001.png";

    @Autowired
    private AdminHomePageService adminHomePageService;

    @Autowired
    private HomePageAdvantageRepository homePageAdvantageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void homePageFlow_ShouldUpdateContentReorderAdvantagesAndEnqueueIconDeletion() {
        adminHomePageService.updateHomePageContent(
                new AdminHomePageContentRequestDto("Updated home title", "Updated home description")
        );

        AdminHomePageAdvantageDto firstAdvantage = adminHomePageService.createAdvantage(
                new AdminHomePageAdvantageRequestDto("Fast delivery", "Within 3 days", ICON_OLD_KEY)
        );
        AdminHomePageAdvantageDto secondAdvantage = adminHomePageService.createAdvantage(
                new AdminHomePageAdvantageRequestDto("Handmade", "Crafted with care", null)
        );

        adminHomePageService.reorderAdvantages(
                new HomeAdvantagesOrderRequestDto(List.of(secondAdvantage.advantageId(), firstAdvantage.advantageId()))
        );

        adminHomePageService.updateAdvantage(
                firstAdvantage.advantageId(),
                new AdminHomePageAdvantageRequestDto("Fast delivery", "Within 3 days", ICON_NEW_KEY)
        );

        assertEquals("Updated home title", findHomePageTitle());
        assertEquals(1, findAdvantageDisplayOrder(secondAdvantage.advantageId()));
        assertEquals(2, findAdvantageDisplayOrder(firstAdvantage.advantageId()));
        assertEquals(1, countStorageDeletionOutboxRows(ICON_OLD_KEY));

        adminHomePageService.deleteAdvantage(firstAdvantage.advantageId());

        assertEquals(1, homePageAdvantageRepository.count());
        assertEquals(1, countStorageDeletionOutboxRows(ICON_NEW_KEY));
    }

    private String findHomePageTitle() {
        return jdbcTemplate.queryForObject("select title from home_page_content where id = ?", String.class, SINGLETON_ID);
    }

    private Integer findAdvantageDisplayOrder(Long advantageId) {
        return jdbcTemplate.queryForObject(
                "select display_order from home_page_advantages where id = ?",
                Integer.class,
                advantageId
        );
    }

    private Integer countStorageDeletionOutboxRows(String s3Key) {
        return jdbcTemplate.queryForObject(
                "select count(*) from storage_deletion_outbox where s3_key = ?",
                Integer.class,
                s3Key
        );
    }
}
