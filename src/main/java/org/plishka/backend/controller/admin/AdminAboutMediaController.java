package org.plishka.backend.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.admin.about.AboutMediaOrderRequestDto;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.service.admin.about.AdminAboutPageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/about")
@RequiredArgsConstructor
public class AdminAboutMediaController {
    private final AdminAboutPageService adminAboutPageService;

    @PostMapping("/media/attach")
    public void attachMedia(@Valid @RequestBody AttachMediaRequestDto request) {
        adminAboutPageService.attachMedia(request);
    }

    @PutMapping("/media/order")
    public void reorderMedia(@Valid @RequestBody AboutMediaOrderRequestDto request) {
        adminAboutPageService.reorderMedia(request);
    }

    @DeleteMapping("/media")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllMedia() {
        adminAboutPageService.deleteAllMedia();
    }

    @DeleteMapping("/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(@Positive @PathVariable Long mediaId) {
        adminAboutPageService.deleteMedia(mediaId);
    }
}
