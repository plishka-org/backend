package org.plishka.backend.controller.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.dto.product.ProductDetailDto;
import org.plishka.backend.dto.product.ProductListRequestDto;
import org.plishka.backend.dto.product.ProductSummaryDto;
import org.plishka.backend.service.product.ProductService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {
    private static final int DEFAULT_PAGE_SIZE = 16;
    private static final int DEFAULT_RELATED_PRODUCTS_PAGE_SIZE = 4;

    private final ProductService productService;

    @Operation(
            operationId = "getProducts",
            summary = "List products",
            description = "Public product listing. Pagination defaults: page=0, size=16. "
                    + "List query parameters are repeated, for example: ?categoryIds=1&categoryIds=2. "
                    + "Supported sort values: name,asc | name,desc | price,asc | price,desc. "
                    + "Price sorting is available only when shop mode is enabled; otherwise returns 400."
    )
    @GetMapping
    public PageResponse<ProductSummaryDto> getProducts(
            @ParameterObject @Valid @ModelAttribute ProductListRequestDto productListRequest,
            @ParameterObject @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return productService.getProducts(
                productListRequest.resolveCategoryIds(),
                productListRequest.sort(),
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @Operation(
            operationId = "getProduct",
            summary = "Get product details",
            description = "Public product details."
    )
    @GetMapping("/{id}")
    public ProductDetailDto getProduct(@Positive @PathVariable Long id) {
        return productService.getProduct(id);
    }

    @Operation(
            operationId = "getRelatedProducts",
            summary = "List related products",
            description = "Public related products listing. Pagination defaults: page=0, size=4."
    )
    @GetMapping("/{id}/related")
    public PageResponse<ProductSummaryDto> getRelatedProducts(
            @Positive @PathVariable Long id,
            @ParameterObject @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return productService.getRelatedProducts(
                id,
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_RELATED_PRODUCTS_PAGE_SIZE)
        );
    }
}
