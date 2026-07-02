package org.plishka.backend.service.admin.home;

import org.plishka.backend.dto.admin.home.AdminHomePageAdvantageDto;
import org.plishka.backend.dto.admin.home.AdminHomePageAdvantageRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageContentRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageDto;
import org.plishka.backend.dto.admin.home.HomeAdvantagesOrderRequestDto;
import org.plishka.backend.dto.home.HomePageContentDto;

public interface AdminHomePageService {
    AdminHomePageDto getHomePage();

    HomePageContentDto updateHomePageContent(AdminHomePageContentRequestDto request);

    AdminHomePageAdvantageDto createAdvantage(AdminHomePageAdvantageRequestDto request);

    AdminHomePageAdvantageDto updateAdvantage(Long advantageId, AdminHomePageAdvantageRequestDto request);

    void deleteAdvantage(Long advantageId);

    void reorderAdvantages(HomeAdvantagesOrderRequestDto request);
}
