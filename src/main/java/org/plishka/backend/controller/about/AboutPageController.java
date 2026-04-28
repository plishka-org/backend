package org.plishka.backend.controller.about;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.about.AboutPageResponse;
import org.plishka.backend.service.about.AboutPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/about")
@RequiredArgsConstructor
public class AboutPageController {
    private final AboutPageService aboutPageService;

    @GetMapping
    public AboutPageResponse getAboutPage() {
        return aboutPageService.getAboutPageData();
    }
}
