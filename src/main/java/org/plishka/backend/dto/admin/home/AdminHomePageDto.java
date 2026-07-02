package org.plishka.backend.dto.admin.home;

import java.util.List;
import org.plishka.backend.dto.home.HomePageContentDto;

public record AdminHomePageDto(
        HomePageContentDto content,
        List<AdminHomePageAdvantageDto> advantages
) {
}
