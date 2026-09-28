package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin - About")
@SecurityRequirement(name = "bearerAuth")
public class AdminAboutPageController {
    private final AdminAboutPageService adminAboutPageService;

    @Operation(
            operationId = "adminGetAboutPage",
            summary = "Admin get about page",
            description = "Requires active user with ROLE_ADMIN."
    )
    @GetMapping
    public AdminAboutPageDto getAboutPage() {
        return adminAboutPageService.getAboutPage();
    }

    @Operation(
            operationId = "adminUpdateAboutPage",
            summary = "Admin update about page content",
            description = "Requires active user with ROLE_ADMIN."
    )
    @PutMapping
    public AboutPageContentDto updateAboutPage(@Valid @RequestBody AdminAboutPageContentRequestDto request) {
        return adminAboutPageService.updateAboutPageContent(request);
    }
}
