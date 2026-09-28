package org.plishka.backend.mapper.callback;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.callback.CallbackRequest;
import org.plishka.backend.dto.callback.CallbackRequestDto;

@Mapper(config = MapStructConfig.class)
public interface CallbackRequestMapper {
    @Mapping(target = "callbackRequestId", source = "id")
    CallbackRequestDto toDto(CallbackRequest callbackRequest);
}
