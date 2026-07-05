package org.plishka.backend.mapper.about;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.about.AboutPageContent;
import org.plishka.backend.domain.about.AboutPageMedia;
import org.plishka.backend.dto.about.AboutPageContentDto;
import org.plishka.backend.dto.about.AboutPageMediaDto;
import org.plishka.backend.dto.about.AboutPageResponse;
import org.plishka.backend.dto.admin.about.AdminAboutPageMediaDto;

@Mapper(config = MapStructConfig.class)
public interface AboutPageMapper {
    @Mapping(target = "content", source = "content")
    AboutPageResponse toResponse(AboutPageContent content, List<AboutPageMedia> media);

    AboutPageContentDto toContentDto(AboutPageContent content);

    @Mapping(target = "aboutPageMediaId", source = "id")
    AboutPageMediaDto toMediaDto(AboutPageMedia media);

    @Mapping(target = "mediaId", source = "id")
    AdminAboutPageMediaDto toAdminMediaDto(AboutPageMedia media);
}
