package org.plishka.backend.config;

import java.time.Clock;
import org.hibernate.generator.internal.CurrentTimestampGeneration;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateClockConfig {
    @Bean
    public HibernatePropertiesCustomizer hibernateClockCustomizer(Clock clock) {
        return hibernateProperties ->
                hibernateProperties.put(CurrentTimestampGeneration.CLOCK_SETTING_NAME, clock);
    }
}
