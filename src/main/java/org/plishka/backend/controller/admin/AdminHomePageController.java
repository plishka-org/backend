package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin - Home")
@SecurityRequirement(name = "bearerAuth")
public class AdminHomePageController {
    private final AdminHomePageService adminHomePageService;

    @Operation(
            operationId = "adminGetHomePage",
            summary = "Admin get home page content",
            description = "Requires active user with ROLE_ADMIN."
    )
    @GetMapping
    public AdminHomePageDto getHomePage() {
        return adminHomePageService.getHomePage();
    }

    @Operation(
            operationId = "adminUpdateHomePage",
            summary = "Admin update home page content",
            description = "Requires active user with ROLE_ADMIN."
    )
    @PutMapping
    public HomePageContentDto updateHomePage(@Valid @RequestBody AdminHomePageContentRequestDto request) {
        return adminHomePageService.updateHomePageContent(request);
    }
}
