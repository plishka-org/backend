package org.plishka.backend.controller.admin;

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
public class AdminProductMediaController {
    private final ProductService productService;
    private final AdminProductMediaService adminProductMediaService;

    @PostMapping("/{id}/media/attach")
    public void attachMedia(@Positive @PathVariable Long id, @Valid @RequestBody AttachMediaRequestDto request) {
        productService.attachMedia(id, request);
    }

    @DeleteMapping("/{productId}/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(
            @Positive @PathVariable Long productId,
            @Positive @PathVariable Long mediaId
    ) {
        adminProductMediaService.deleteMedia(productId, mediaId);
    }

    @DeleteMapping("/{productId}/media")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllMedia(@Positive @PathVariable Long productId) {
        adminProductMediaService.deleteAllMedia(productId);
    }

    @PutMapping("/{productId}/media/{mediaId}/primary")
    public void markPrimary(
            @Positive @PathVariable Long productId,
            @Positive @PathVariable Long mediaId
    ) {
        adminProductMediaService.markPrimary(productId, mediaId);
    }
}
