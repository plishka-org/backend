package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.admin.common.BulkOperationResultDto;
import org.plishka.backend.dto.admin.home.HomeProductsRequestDto;
import org.plishka.backend.dto.admin.product.AdminProductDetailDto;
import org.plishka.backend.dto.admin.product.AdminProductRequestDto;
import org.plishka.backend.dto.admin.product.AdminProductSearchRequestDto;
import org.plishka.backend.dto.admin.product.BulkProductCategoryRequestDto;
import org.plishka.backend.dto.admin.product.BulkProductDeleteRequestDto;
import org.plishka.backend.dto.admin.product.BulkProductPriceRequestDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.service.admin.catalog.home.AdminHomeProductService;
import org.plishka.backend.service.admin.catalog.product.AdminProductService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
@Tag(name = "Admin - Products")
@SecurityRequirement(name = "bearerAuth")
public class AdminProductController {
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AdminProductService adminProductService;
    private final AdminHomeProductService adminHomeProductService;

    @Operation(
            operationId = "adminGetProducts",
            summary = "Admin list products",
            description = "Requires active user with ROLE_ADMIN. Pagination defaults: page=0, size=20. "
                    + "List query parameters are repeated, for example: ?categoryIds=1&categoryIds=2. "
                    + "Supported sort values: name,asc | name,desc | price,asc | price,desc."
    )
    @GetMapping
    public PageResponse<AdminProductDetailDto> getProducts(
            @ParameterObject @Valid @ModelAttribute AdminProductSearchRequestDto productRequest,
            @ParameterObject @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return adminProductService.getProducts(
                productRequest,
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @Operation(
            operationId = "adminGetProduct",
            summary = "Admin get product",
            description = "Requires active user with ROLE_ADMIN."
    )
    @GetMapping("/{id}")
    public AdminProductDetailDto getProduct(@Positive @PathVariable Long id) {
        return adminProductService.getProduct(id);
    }

    @Operation(
            operationId = "adminCreateProduct",
            summary = "Admin create product",
            description = "Requires active user with ROLE_ADMIN. Price is an integer amount in whole Ukrainian "
                    + "hryvnias (UAH), without fractional kopiykas."
    )
    @ApiResponse(responseCode = "201", description = "Product created.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductDetailDto createProduct(@Valid @RequestBody AdminProductRequestDto request) {
        return adminProductService.createProduct(request);
    }

    @Operation(
            operationId = "adminUpdateProduct",
            summary = "Admin update product",
            description = "Requires active user with ROLE_ADMIN. Price is an integer amount in whole Ukrainian "
                    + "hryvnias (UAH), without fractional kopiykas."
    )
    @PutMapping("/{id}")
    public AdminProductDetailDto updateProduct(
            @Positive @PathVariable Long id,
            @Valid @RequestBody AdminProductRequestDto request
    ) {
        return adminProductService.updateProduct(id, request);
    }

    @Operation(
            operationId = "adminDeleteProduct",
            summary = "Admin delete product",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(responseCode = "204", description = "Product deleted.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@Positive @PathVariable Long id) {
        adminProductService.deleteProduct(id);
    }

    @Operation(
            operationId = "adminBulkDeleteProducts",
            summary = "Admin bulk delete products",
            description = "Requires active user with ROLE_ADMIN. For selectionMode=EXCEPT_SELECTED, "
                    + "the operation applies to all matching filters except productIds."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "selectionMode": "EXCEPT_SELECTED",
                      "productIds": [10, 11],
                      "filters": {
                        "categoryIds": [1, 2],
                        "uncategorized": false,
                        "search": "storage box"
                      }
                    }
                    """))
    )
    @PostMapping("/bulk/delete")
    public BulkOperationResultDto deleteProducts(@Valid @RequestBody BulkProductDeleteRequestDto request) {
        return adminProductService.deleteProducts(request);
    }

    @Operation(
            operationId = "adminBulkUpdateProductPrices",
            summary = "Admin bulk update product prices",
            description = "Requires active user with ROLE_ADMIN. For selectionMode=EXCEPT_SELECTED, "
                    + "the operation applies to all matching filters except productIds. Amount values are whole UAH; "
                    + "percent values are integer percentages."
    )
    @PostMapping("/bulk/price")
    public BulkOperationResultDto updatePrices(@Valid @RequestBody BulkProductPriceRequestDto request) {
        return adminProductService.updatePrices(request);
    }

    @Operation(
            operationId = "adminBulkUpdateProductCategories",
            summary = "Admin bulk update product categories",
            description = "Requires active user with ROLE_ADMIN. For selectionMode=EXCEPT_SELECTED, "
                    + "the operation applies to all matching filters except productIds."
    )
    @PostMapping("/bulk/category")
    public BulkOperationResultDto updateCategories(@Valid @RequestBody BulkProductCategoryRequestDto request) {
        return adminProductService.updateCategories(request);
    }

    @Operation(
            operationId = "adminReplaceHomeProducts",
            summary = "Admin replace home products",
            description = "Requires active user with ROLE_ADMIN. Replaces the product list displayed on the home page."
    )
    @ApiResponse(responseCode = "200", description = "Home products replaced; empty response body.", content = @Content)
    @PutMapping("/home")
    public void replaceHomeProducts(@Valid @RequestBody HomeProductsRequestDto request) {
        adminHomeProductService.replaceHomeProducts(request);
    }

    @Operation(
            operationId = "adminReorderHomeProducts",
            summary = "Admin reorder home products",
            description = "Requires active user with ROLE_ADMIN. Reorders existing home page products."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Home products reordered; empty response body.",
            content = @Content
    )
    @PutMapping("/home-order")
    public void reorderHomeProducts(@Valid @RequestBody HomeProductsRequestDto request) {
        adminHomeProductService.reorderHomeProducts(request);
    }
}
