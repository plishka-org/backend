package org.plishka.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.openapi.customizer.OperationDocumentationCustomizer;
import org.plishka.backend.openapi.operation.OperationDocumentationRegistry;
import org.plishka.backend.openapi.support.OpenApiExamples;
import org.plishka.backend.openapi.support.OpenApiResponses;
import org.plishka.backend.openapi.support.OpenApiSchemas;
import org.plishka.backend.service.VersionService;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
@RequiredArgsConstructor
public class OpenApiConfig {
    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    private final VersionService versionService;

    @Bean
    public OpenAPI plishkaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plishka Backend API")
                        .description("API for Plishka storefront, user account flows, shopping cart, orders, "
                                + "media files, and admin panel operations.")
                        .version(String.valueOf(versionService.getCurrentVersion())))
                .servers(List.of(new Server().url("/api")))
                .tags(List.of(
                        tag("Auth"),
                        tag("Users"),
                        tag("Cart"),
                        tag("Orders"),
                        tag("Products"),
                        tag("Categories"),
                        tag("Reviews"),
                        tag("Favorites"),
                        tag("Callback"),
                        tag("Files"),
                        tag("Settings"),
                        tag("Home"),
                        tag("About"),
                        tag("Contacts"),
                        tag("Admin - About"),
                        tag("Admin - Categories"),
                        tag("Admin - Contacts"),
                        tag("Admin - Files"),
                        tag("Admin - Home"),
                        tag("Admin - Product Media"),
                        tag("Admin - Products"),
                        tag("Admin - Review Media"),
                        tag("Admin - Reviews"),
                        tag("Admin - Settings"),
                        tag("Admin - Users"),
                        tag("Version")
                ))
                .components(openApiComponents());
    }

    @Bean
    public OperationDocumentationRegistry operationDocumentationRegistry() {
        return new OperationDocumentationRegistry();
    }

    @Bean
    public OpenApiExamples openApiExamples() {
        return new OpenApiExamples();
    }

    @Bean
    public OpenApiResponses openApiResponses(OpenApiExamples examples) {
        return new OpenApiResponses(examples);
    }

    @Bean
    public OpenApiSchemas openApiSchemas() {
        return new OpenApiSchemas();
    }

    @Bean
    public OperationDocumentationCustomizer operationDocumentationCustomizer(
            OperationDocumentationRegistry registry,
            OpenApiExamples examples,
            OpenApiResponses responseFactory
    ) {
        return new OperationDocumentationCustomizer(registry, examples, responseFactory);
    }

    @Bean
    public OpenApiCustomizer plishkaSchemaCustomizer(OpenApiSchemas schemas) {
        return schemas::addErrorResponseSchemas;
    }

    @Bean
    public OpenApiCustomizer plishkaOperationCustomizer(OperationDocumentationCustomizer customizer) {
        return customizer::customize;
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT access token in the Authorization header: Bearer <accessToken>.");
    }

    private Components openApiComponents() {
        return new Components()
                .addSecuritySchemes(BEARER_AUTH_SCHEME, bearerAuthScheme());
    }

    private Tag tag(String name) {
        return new Tag().name(name);
    }
}
