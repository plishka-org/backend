package org.plishka.backend.service.product;

import java.util.List;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.dto.product.ProductDetailDto;
import org.plishka.backend.dto.product.ProductSummaryDto;

public interface ProductService {
    PageResponse<ProductSummaryDto> getProducts(List<Long> categoryIds, String sort, int page, int size);

    ProductDetailDto getProduct(Long id);

    PageResponse<ProductSummaryDto> getRelatedProducts(Long id, int page, int size);

    void attachMedia(Long productId, AttachMediaRequestDto request);
}
