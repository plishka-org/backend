package org.plishka.backend.security;

import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

@Component
public class ActiveUserAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    @Override
    public AuthorizationDecision authorize(
            Supplier<? extends Authentication> authenticationSupplier,
            RequestAuthorizationContext context
    ) {
        Authentication authentication = authenticationSupplier.get();
        boolean granted = authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails userDetails
                && userDetails.isEnabled()
                && userDetails.isAccountNonLocked();

        return new AuthorizationDecision(granted);
    }
}
