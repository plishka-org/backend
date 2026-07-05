package org.plishka.backend.security;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.plishka.backend.exception.ShopModeDisabledAccessDeniedException;
import org.plishka.backend.service.settings.ShopModeService;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShopModeAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private final ShopModeService shopModeService;

    @Override
    public AuthorizationDecision authorize(
            @NonNull Supplier<? extends Authentication> authenticationSupplier,
            RequestAuthorizationContext context
    ) {
        if (!shopModeService.isShopModeEnabled()) {
            throw new ShopModeDisabledAccessDeniedException();
        }

        return new AuthorizationDecision(true);
    }
}
