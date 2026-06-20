package org.plishka.backend.service.user;

import org.plishka.backend.domain.user.User;

public interface EmailChangeService {
    void initiateEmailChange(User user, String newEmail);

    void verifyEmailChange(String token);
}
