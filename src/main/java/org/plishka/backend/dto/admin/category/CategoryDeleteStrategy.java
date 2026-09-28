package org.plishka.backend.dto.admin.category;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category deletion strategy. targetCategoryId is required only with MOVE_PRODUCTS.")
public enum CategoryDeleteStrategy {
    KEEP_PRODUCTS,
    MOVE_PRODUCTS,
    DELETE_PRODUCTS
}
