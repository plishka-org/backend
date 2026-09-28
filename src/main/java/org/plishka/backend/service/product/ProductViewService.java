package org.plishka.backend.service.product;

import java.util.List;
import org.plishka.backend.dto.product.ProductViewDto;

public interface ProductViewService {
    ProductViewDto recordProductView(Long userId, Long productId);

    List<ProductViewDto> getViewedProducts(Long userId);
}
