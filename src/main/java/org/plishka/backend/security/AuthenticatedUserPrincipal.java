package org.plishka.backend.security;

import java.util.Collection;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor
@Builder
public class AuthenticatedUserPrincipal implements UserDetails {
    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final boolean accountNonLocked;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public @NonNull String getUsername() {
        return email;
    }
}
