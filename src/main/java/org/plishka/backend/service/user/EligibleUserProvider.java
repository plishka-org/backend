package org.plishka.backend.service.user;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.exception.EmailNotVerifiedException;
import org.plishka.backend.exception.ForbiddenException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.user.UserRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EligibleUserProvider {
    private static final String USER_NOT_FOUND_MESSAGE = "User with ID '%d' not found";

    private final UserRepository userRepository;

    public User getEligibleUserOrThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE.formatted(userId)));

        validate(user);
        return user;
    }

    public User getEligibleUserForUpdateOrThrow(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE.formatted(userId)));

        validate(user);
        return user;
    }

    private void validate(User user) {
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email is not verified");
        }

        if (user.isBanned()) {
            throw new ForbiddenException("User is banned");
        }
    }
}
