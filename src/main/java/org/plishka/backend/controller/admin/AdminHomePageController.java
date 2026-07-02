package org.plishka.backend.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.admin.home.AdminHomePageAdvantageDto;
import org.plishka.backend.dto.admin.home.AdminHomePageAdvantageRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageContentRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageDto;
import org.plishka.backend.dto.admin.home.HomeAdvantagesOrderRequestDto;
import org.plishka.backend.dto.home.HomePageContentDto;
import org.plishka.backend.service.admin.home.AdminHomePageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping("/advantages")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminHomePageAdvantageDto createAdvantage(@Valid @RequestBody AdminHomePageAdvantageRequestDto request) {
        return adminHomePageService.createAdvantage(request);
    }

    @PutMapping("/advantages/order")
    public void reorderAdvantages(@Valid @RequestBody HomeAdvantagesOrderRequestDto request) {
        adminHomePageService.reorderAdvantages(request);
    }

    @PutMapping("/advantages/{advantageId}")
    public AdminHomePageAdvantageDto updateAdvantage(
            @Positive @PathVariable Long advantageId,
            @Valid @RequestBody AdminHomePageAdvantageRequestDto request
    ) {
        return adminHomePageService.updateAdvantage(advantageId, request);
    }

    @DeleteMapping("/advantages/{advantageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAdvantage(@Positive @PathVariable Long advantageId) {
        adminHomePageService.deleteAdvantage(advantageId);
    }
}
