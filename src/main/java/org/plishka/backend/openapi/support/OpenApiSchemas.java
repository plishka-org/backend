package org.plishka.backend.openapi.support;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import java.lang.reflect.Type;
import org.plishka.backend.dto.common.ErrorResponseDto;
import org.plishka.backend.dto.common.ValidationErrorResponseDto;

public class OpenApiSchemas {
    public void addErrorResponseSchemas(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }

        addComponentSchemas(components, ErrorResponseDto.class);
        addComponentSchemas(components, ValidationErrorResponseDto.class);
    }

    private void addComponentSchemas(Components components, Type dtoType) {
        ModelConverters.getInstance().readAll(dtoType).forEach(components::addSchemas);
    }
}
