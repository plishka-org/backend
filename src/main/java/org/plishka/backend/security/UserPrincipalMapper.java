package org.plishka.backend.security;

import java.util.List;
import org.plishka.backend.domain.user.User;
import org.springframework.security.core.GrantedAuthority;

public final class UserPrincipalMapper {
    private UserPrincipalMapper() {
    }

    public static AuthenticatedUserPrincipal toPrincipal(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(GrantedAuthority.class::cast)
                .toList();

        return AuthenticatedUserPrincipal.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .accountNonLocked(!user.isBanned())
                .enabled(user.isEmailVerified())
                .authorities(authorities)
                .build();
    }
}
