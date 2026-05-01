package org.plishka.backend.controller.about;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.about.AboutPageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.service.about.AboutPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/media/attach")
    public void attachMedia(@Valid @RequestBody AttachMediaRequestDto request) {
        aboutPageService.attachMedia(request);
    }
}
