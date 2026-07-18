package org.plishka.backend.openapi.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.config.OpenApiConfig;
import org.plishka.backend.openapi.operation.ForbiddenMode;
import org.plishka.backend.openapi.operation.OperationDocumentation;
import org.plishka.backend.openapi.operation.OperationDocumentationRegistry;
import org.plishka.backend.openapi.operation.UnauthorizedMode;
import org.plishka.backend.openapi.support.OpenApiExamples;
import org.plishka.backend.openapi.support.OpenApiResponses;

@RequiredArgsConstructor
public class OperationDocumentationCustomizer {
    private static final List<String> MANAGED_ERROR_CODES =
            List.of("400", "401", "403", "404", "409", "429", "500", "503");

    private final OperationDocumentationRegistry registry;
    private final OpenApiExamples examples;
    private final OpenApiResponses responseFactory;

    public void customize(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }

        openApi.getPaths().remove("/error");
        openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperations().forEach(operation -> documentOperation(path, operation)));
    }

    private void documentOperation(String path, Operation operation) {
        ApiResponses apiResponses = ensureResponses(operation);
        OperationDocumentation documentation = registry.get(operation.getOperationId());

        resetManagedResponses(apiResponses);
        examples.addExamples(operation, documentation);
        addStandardErrorResponses(path, apiResponses);
        addDocumentedErrorResponses(path, operation, apiResponses, documentation);
    }

    private ApiResponses ensureResponses(Operation operation) {
        ApiResponses apiResponses = operation.getResponses();
        if (apiResponses == null) {
            apiResponses = new ApiResponses();
            operation.setResponses(apiResponses);
        }

        return apiResponses;
    }

    private void resetManagedResponses(ApiResponses apiResponses) {
        MANAGED_ERROR_CODES.forEach(apiResponses::remove);
    }

    private void addStandardErrorResponses(String path, ApiResponses apiResponses) {
        apiResponses.put("429", responseFactory.tooManyRequestsResponse(path));
        apiResponses.put("500", responseFactory.internalServerErrorResponse(path));
    }

    private void addDocumentedErrorResponses(
            String path,
            Operation operation,
            ApiResponses apiResponses,
            OperationDocumentation documentation
    ) {
        if (documentation.hasBadRequest()) {
            apiResponses.put("400", responseFactory.badRequestResponse(path, documentation));
        }

        if (documentation.secured()) {
            addBearerSecurityIfAbsent(operation);
        }

        if (documentation.unauthorizedMode() != UnauthorizedMode.NONE) {
            apiResponses.put("401", responseFactory.unauthorizedResponse(path, documentation.unauthorizedMode()));
        }

        if (documentation.forbiddenMode() != ForbiddenMode.NONE) {
            apiResponses.put("403", responseFactory.forbiddenResponse(path, documentation.forbiddenMode()));
        }

        if (documentation.hasNotFound()) {
            apiResponses.put("404", responseFactory.notFoundResponse(path, documentation));
        }

        if (documentation.hasConflict()) {
            apiResponses.put("409", responseFactory.conflictResponse(path, documentation));
        }

        if (documentation.storageUnavailable()) {
            apiResponses.put("503", responseFactory.storageUnavailableResponse(path));
        }
    }

    private void addBearerSecurityIfAbsent(Operation operation) {
        List<SecurityRequirement> security = operation.getSecurity();
        if (security != null
                && security.stream().anyMatch(requirement ->
                        requirement.containsKey(OpenApiConfig.BEARER_AUTH_SCHEME))) {
            return;
        }

        operation.addSecurityItem(new SecurityRequirement().addList(OpenApiConfig.BEARER_AUTH_SCHEME));
    }
}
