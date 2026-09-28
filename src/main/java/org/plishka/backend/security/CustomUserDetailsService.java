package org.plishka.backend.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) {
        String normalizedEmail = UserInputNormalizer.normalizeEmail(email);
        User user = userRepository.findWithRolesByEmail(normalizedEmail).orElseThrow(
                () -> new UsernameNotFoundException("User with email '%s' not found".formatted(normalizedEmail))
        );

        return UserPrincipalMapper.toPrincipal(user);
    }
}
