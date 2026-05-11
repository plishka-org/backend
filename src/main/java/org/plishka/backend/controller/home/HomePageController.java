package org.plishka.backend.controller.home;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.home.HomePageResponse;
import org.plishka.backend.service.home.HomePageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomePageController {
    private final HomePageService homePageService;

    @GetMapping
    public HomePageResponse getHomePage() {
        return homePageService.getHomePageData();
    }
}
