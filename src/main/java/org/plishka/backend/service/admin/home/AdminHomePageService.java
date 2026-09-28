package org.plishka.backend.service.admin.home;

import org.plishka.backend.dto.admin.home.AdminHomePageContentRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageDto;
import org.plishka.backend.dto.home.HomePageContentDto;

public interface AdminHomePageService {
    AdminHomePageDto getHomePage();

    HomePageContentDto updateHomePageContent(AdminHomePageContentRequestDto request);
}
