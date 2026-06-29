package org.plishka.backend.security;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.user.Role;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.repository.user.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_ShouldNormalizeEmailBeforeLookup() {
        User user = user(1L, "user@example.com");
        when(userRepository.findWithRolesByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));

        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) service.loadUserByUsername(
                "  USER@Example.com  "
        );

        assertEquals("user@example.com", principal.getUsername());
        verify(userRepository).findWithRolesByEmail("user@example.com");
    }

    private static User user(Long id, String email) {
        Role role = new Role();
        role.setName(Role.RoleName.USER);

        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRoles(Set.of(role));
        return user;
    }
}
