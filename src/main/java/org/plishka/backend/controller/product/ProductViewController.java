package org.plishka.backend.controller.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.product.ProductViewDto;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.service.product.ProductViewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Products")
@SecurityRequirement(name = "bearerAuth")
public class ProductViewController {
    private final ProductViewService productViewService;

    @Operation(
            operationId = "recordProductView",
            summary = "Record product view",
            description = "Requires an active user: authenticated, email verified, and not banned."
    )
    @PostMapping("/products/{id}/view")
    public ProductViewDto recordProductView(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Positive @PathVariable Long id
    ) {
        return productViewService.recordProductView(principal.getUserId(), id);
    }

    @Operation(
            operationId = "getViewedProducts",
            summary = "List viewed products",
            description = "Requires an active user: authenticated, email verified, and not banned.",
            tags = "Users"
    )
    @GetMapping("/users/me/viewed")
    public List<ProductViewDto> getViewedProducts(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return productViewService.getViewedProducts(principal.getUserId());
    }
}
