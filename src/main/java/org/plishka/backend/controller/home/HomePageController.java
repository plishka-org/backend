package org.plishka.backend.controller.home;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.home.HomePageResponse;
import org.plishka.backend.service.home.HomePageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
@Tag(name = "Home")
public class HomePageController {
    private final HomePageService homePageService;

    @Operation(
            operationId = "getHomePage",
            summary = "Get home page content",
            description = "Public home page content, storefront products, and featured reviews. "
                    + "Featured reviews are limited to a maximum of 5."
    )
    @GetMapping
    public HomePageResponse getHomePage() {
        return homePageService.getHomePageData();
    }
}
