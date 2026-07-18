package org.plishka.backend.service.settings;

public interface ShopModeService {
    String SHOP_MODE_DISABLED_MESSAGE = "Shop mode is disabled";

    boolean isShopModeEnabled();

    void requireEnabled();
}
