package org.plishka.backend.service.admin.catalog.product;

import org.plishka.backend.dto.admin.common.BulkOperationResultDto;
import org.plishka.backend.dto.admin.product.AdminProductDetailDto;
import org.plishka.backend.dto.admin.product.AdminProductListRequestDto;
import org.plishka.backend.dto.admin.product.AdminProductRequestDto;
import org.plishka.backend.dto.admin.product.BulkProductCategoryRequestDto;
import org.plishka.backend.dto.admin.product.BulkProductDeleteRequestDto;
import org.plishka.backend.dto.admin.product.BulkProductPriceRequestDto;
import org.plishka.backend.dto.common.PageResponse;

public interface AdminProductService {
    PageResponse<AdminProductDetailDto> getProducts(AdminProductListRequestDto request, int page, int size);

    AdminProductDetailDto getProduct(Long productId);

    AdminProductDetailDto createProduct(AdminProductRequestDto request);

    AdminProductDetailDto updateProduct(Long productId, AdminProductRequestDto request);

    void deleteProduct(Long productId);

    BulkOperationResultDto deleteProducts(BulkProductDeleteRequestDto request);

    BulkOperationResultDto updatePrices(BulkProductPriceRequestDto request);

    BulkOperationResultDto updateCategories(BulkProductCategoryRequestDto request);
}
