package org.plishka.backend.controller.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.dto.product.ProductDetailDto;
import org.plishka.backend.dto.product.ProductListRequestDto;
import org.plishka.backend.dto.product.ProductSummaryDto;
import org.plishka.backend.service.product.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private static final int DEFAULT_PAGE_SIZE = 16;
    private static final int DEFAULT_RELATED_PRODUCTS_PAGE_SIZE = 4;

    private final ProductService productService;

    @GetMapping
    public PageResponse<ProductSummaryDto> getProducts(
            @Valid @ModelAttribute ProductListRequestDto productListRequest,
            @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return productService.getProducts(
                productListRequest.resolveCategoryIds(),
                productListRequest.sort(),
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @GetMapping("/{id}")
    public ProductDetailDto getProduct(@Positive @PathVariable Long id) {
        return productService.getProduct(id);
    }

    @GetMapping("/{id}/related")
    public PageResponse<ProductSummaryDto> getRelatedProducts(
            @Positive @PathVariable Long id,
            @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return productService.getRelatedProducts(
                id,
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_RELATED_PRODUCTS_PAGE_SIZE)
        );
    }
}
