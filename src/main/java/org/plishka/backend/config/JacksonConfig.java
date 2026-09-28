package org.plishka.backend.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamReadFeature;

@Configuration
public class JacksonConfig {
    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return jsonMapperBuilder -> jsonMapperBuilder.configure(
                StreamReadFeature.STRICT_DUPLICATE_DETECTION,
                true
        );
    }
}
