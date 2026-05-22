package org.plishka.backend.mapper.settings;

import org.mapstruct.Mapper;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.settings.SystemSettings;
import org.plishka.backend.dto.settings.SystemSettingsDto;

@Mapper(config = MapStructConfig.class)
public interface SystemSettingsMapper {
    SystemSettingsDto toDto(SystemSettings settings);
}
