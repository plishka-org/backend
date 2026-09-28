package org.plishka.backend.exception;

import org.plishka.backend.service.settings.ShopModeService;
import org.springframework.security.access.AccessDeniedException;

public class ShopModeDisabledAccessDeniedException extends AccessDeniedException {
    public ShopModeDisabledAccessDeniedException() {
        super(ShopModeService.SHOP_MODE_DISABLED_MESSAGE);
    }
}
