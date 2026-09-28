package org.plishka.backend.dto.admin.product;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bulk price operation.")
public enum BulkProductPriceOperation {
    INCREASE_PERCENT,
    DECREASE_PERCENT,
    INCREASE_AMOUNT,
    DECREASE_AMOUNT
}
