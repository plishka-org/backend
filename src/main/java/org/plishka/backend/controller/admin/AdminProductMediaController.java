package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.service.admin.catalog.product.AdminProductMediaService;
import org.plishka.backend.service.product.ProductService;
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
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Tag(name = "Admin - Product Media")
@SecurityRequirement(name = "bearerAuth")
public class AdminProductMediaController {
    private final ProductService productService;
    private final AdminProductMediaService adminProductMediaService;

    @Operation(
            operationId = "adminAttachProductMedia",
            summary = "Admin attach product media",
            description = "Requires active user with ROLE_ADMIN. S3 keys are opaque strings for clients."
    )
    @ApiResponse(responseCode = "200", description = "Product media attached; empty response body.", content = @Content)
    @PostMapping("/{id}/media/attach")
    public void attachMedia(@Positive @PathVariable Long id, @Valid @RequestBody AttachMediaRequestDto request) {
        productService.attachMedia(id, request);
    }

    @Operation(
            operationId = "adminDeleteProductMedia",
            summary = "Admin delete product media",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(responseCode = "204", description = "Product media deleted.")
    @DeleteMapping("/{productId}/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(
            @Positive @PathVariable Long productId,
            @Positive @PathVariable Long mediaId
    ) {
        adminProductMediaService.deleteMedia(productId, mediaId);
    }

    @Operation(
            operationId = "adminDeleteAllProductMedia",
            summary = "Admin delete all product media",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(responseCode = "204", description = "All product media deleted.")
    @DeleteMapping("/{productId}/media")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllMedia(@Positive @PathVariable Long productId) {
        adminProductMediaService.deleteAllMedia(productId);
    }

    @Operation(
            operationId = "adminMarkProductMediaPrimary",
            summary = "Admin mark product media as primary",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product media marked as primary; empty response body.",
            content = @Content
    )
    @PutMapping("/{productId}/media/{mediaId}/primary")
    public void markPrimary(
            @Positive @PathVariable Long productId,
            @Positive @PathVariable Long mediaId
    ) {
        adminProductMediaService.markPrimary(productId, mediaId);
    }
}
