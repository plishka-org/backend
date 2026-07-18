package org.plishka.backend.controller.about;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.about.AboutPageResponse;
import org.plishka.backend.service.about.AboutPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/about")
@RequiredArgsConstructor
@Tag(name = "About")
public class AboutPageController {
    private final AboutPageService aboutPageService;

    @Operation(
            operationId = "getAboutPage",
            summary = "Get about page content",
            description = "Public about page content and media."
    )
    @GetMapping
    public AboutPageResponse getAboutPage() {
        return aboutPageService.getAboutPageData();
    }
}
