package org.plishka.backend.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.admin.home.AdminHomePageContentRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageDto;
import org.plishka.backend.dto.home.HomePageContentDto;
import org.plishka.backend.service.admin.home.AdminHomePageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/home-page")
@RequiredArgsConstructor
public class AdminHomePageController {
    private final AdminHomePageService adminHomePageService;

    @GetMapping
    public AdminHomePageDto getHomePage() {
        return adminHomePageService.getHomePage();
    }

    @PutMapping
    public HomePageContentDto updateHomePage(@Valid @RequestBody AdminHomePageContentRequestDto request) {
        return adminHomePageService.updateHomePageContent(request);
    }
}
