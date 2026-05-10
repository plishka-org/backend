package org.plishka.backend.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.service.about.AboutPageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/about/media")
@RequiredArgsConstructor
public class AdminAboutMediaController {
    private final AboutPageService aboutPageService;

    @PostMapping("/attach")
    public void attachMedia(@Valid @RequestBody AttachMediaRequestDto request) {
        aboutPageService.attachMedia(request);
    }
}
