package org.plishka.backend.service.user;

import org.plishka.backend.dto.user.ChangePasswordRequestDto;
import org.plishka.backend.dto.user.DeleteAccountRequestDto;

public interface UserAccountService {
    void changePassword(Long userId, ChangePasswordRequestDto request);

    void deleteAccount(Long userId, DeleteAccountRequestDto request);
}
