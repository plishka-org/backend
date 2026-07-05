package org.plishka.backend.controller.admin;

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
public class AdminProductController {
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AdminProductService adminProductService;
    private final AdminHomeProductService adminHomeProductService;

    @GetMapping
    public PageResponse<AdminProductDetailDto> getProducts(
            @Valid @ModelAttribute AdminProductSearchRequestDto productRequest,
            @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return adminProductService.getProducts(
                productRequest,
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @GetMapping("/{id}")
    public AdminProductDetailDto getProduct(@Positive @PathVariable Long id) {
        return adminProductService.getProduct(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductDetailDto createProduct(@Valid @RequestBody AdminProductRequestDto request) {
        return adminProductService.createProduct(request);
    }

    @PutMapping("/{id}")
    public AdminProductDetailDto updateProduct(
            @Positive @PathVariable Long id,
            @Valid @RequestBody AdminProductRequestDto request
    ) {
        return adminProductService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@Positive @PathVariable Long id) {
        adminProductService.deleteProduct(id);
    }

    @PostMapping("/bulk/delete")
    public BulkOperationResultDto deleteProducts(@Valid @RequestBody BulkProductDeleteRequestDto request) {
        return adminProductService.deleteProducts(request);
    }

    @PostMapping("/bulk/price")
    public BulkOperationResultDto updatePrices(@Valid @RequestBody BulkProductPriceRequestDto request) {
        return adminProductService.updatePrices(request);
    }

    @PostMapping("/bulk/category")
    public BulkOperationResultDto updateCategories(@Valid @RequestBody BulkProductCategoryRequestDto request) {
        return adminProductService.updateCategories(request);
    }

    @PutMapping("/home")
    public void replaceHomeProducts(@Valid @RequestBody HomeProductsRequestDto request) {
        adminHomeProductService.replaceHomeProducts(request);
    }

    @PutMapping("/home-order")
    public void reorderHomeProducts(@Valid @RequestBody HomeProductsRequestDto request) {
        adminHomeProductService.reorderHomeProducts(request);
    }
}
