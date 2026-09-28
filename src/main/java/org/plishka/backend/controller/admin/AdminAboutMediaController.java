package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin - About")
@SecurityRequirement(name = "bearerAuth")
public class AdminAboutMediaController {
    private final AdminAboutPageService adminAboutPageService;

    @Operation(
            operationId = "adminAttachAboutMedia",
            summary = "Admin attach about page media",
            description = "Requires active user with ROLE_ADMIN. S3 keys are opaque strings for clients."
    )
    @ApiResponse(
            responseCode = "200",
            description = "About page media attached; empty response body.",
            content = @Content
    )
    @PostMapping("/media/attach")
    public void attachMedia(@Valid @RequestBody AttachMediaRequestDto request) {
        adminAboutPageService.attachMedia(request);
    }

    @Operation(
            operationId = "adminReorderAboutMedia",
            summary = "Admin reorder about page media",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(
            responseCode = "200",
            description = "About page media reordered; empty response body.",
            content = @Content
    )
    @PutMapping("/media/order")
    public void reorderMedia(@Valid @RequestBody AboutMediaOrderRequestDto request) {
        adminAboutPageService.reorderMedia(request);
    }

    @Operation(
            operationId = "adminDeleteAllAboutMedia",
            summary = "Admin delete all about page media",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(responseCode = "204", description = "All about page media deleted.")
    @DeleteMapping("/media")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllMedia() {
        adminAboutPageService.deleteAllMedia();
    }

    @Operation(
            operationId = "adminDeleteAboutMedia",
            summary = "Admin delete about page media",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(responseCode = "204", description = "About page media deleted.")
    @DeleteMapping("/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(@Positive @PathVariable Long mediaId) {
        adminAboutPageService.deleteMedia(mediaId);
    }
}
