package org.plishka.backend.controller.product;

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
public class ProductViewController {
    private final ProductViewService productViewService;

    @PostMapping("/products/{id}/view")
    public ProductViewDto recordProductView(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Positive @PathVariable Long id
    ) {
        return productViewService.recordProductView(principal.getUserId(), id);
    }

    @GetMapping("/users/me/viewed")
    public List<ProductViewDto> getViewedProducts(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return productViewService.getViewedProducts(principal.getUserId());
    }
}
