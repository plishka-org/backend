package org.plishka.backend.dto.home;

import java.util.List;

public record HomePageResponse(
        HomePageContentDto content,
        List<HomePageAdvantageDto> advantages,
        List<HomePageProductDto> products
) {
}
