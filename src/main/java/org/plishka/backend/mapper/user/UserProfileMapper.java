package org.plishka.backend.mapper.user;

import org.mapstruct.Mapper;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.user.UserProfileDto;
import org.plishka.backend.dto.user.UserProfileUpdateResponseDto;

@Mapper(config = MapStructConfig.class)
public interface UserProfileMapper {
    UserProfileDto toDto(User user);

    default UserProfileUpdateResponseDto toUpdateResponse(User user) {
        return new UserProfileUpdateResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone()
        );
    }
}
