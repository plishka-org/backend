package org.plishka.backend.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.about.AboutPageContentDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageContentRequestDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageDto;
import org.plishka.backend.service.admin.about.AdminAboutPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/about")
@RequiredArgsConstructor
public class AdminAboutPageController {
    private final AdminAboutPageService adminAboutPageService;

    @GetMapping
    public AdminAboutPageDto getAboutPage() {
        return adminAboutPageService.getAboutPage();
    }

    @PutMapping
    public AboutPageContentDto updateAboutPage(@Valid @RequestBody AdminAboutPageContentRequestDto request) {
        return adminAboutPageService.updateAboutPageContent(request);
    }
}
