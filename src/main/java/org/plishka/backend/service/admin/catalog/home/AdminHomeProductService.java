package org.plishka.backend.service.admin.catalog.home;

import org.plishka.backend.dto.admin.home.HomeProductsRequestDto;

public interface AdminHomeProductService {
    void replaceHomeProducts(HomeProductsRequestDto request);

    void reorderHomeProducts(HomeProductsRequestDto request);
}
