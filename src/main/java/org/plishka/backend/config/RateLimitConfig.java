package org.plishka.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import java.time.Clock;
import org.plishka.backend.config.properties.RateLimitProperties;
import org.plishka.backend.ratelimit.RateLimitFilter;
import org.plishka.backend.ratelimit.RateLimitRequestBodyExtractor;
import org.plishka.backend.ratelimit.RateLimitRuleResolver;
import org.plishka.backend.ratelimit.RateLimitService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RateLimitConfig {
    @Bean
    public Cache<String, Bucket> rateLimitBucketCache(RateLimitProperties properties) {
        return Caffeine.newBuilder()
                .expireAfterAccess(properties.cache().expireAfterAccess())
                .maximumSize(properties.cache().maximumSize())
                .build();
    }

    @Bean
    public RateLimitFilter rateLimitFilter(
            RateLimitRuleResolver ruleResolver,
            RateLimitRequestBodyExtractor requestBodyExtractor,
            RateLimitService rateLimitService,
            RateLimitProperties properties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        return new RateLimitFilter(
                ruleResolver,
                requestBodyExtractor,
                rateLimitService,
                properties,
                objectMapper,
                clock
        );
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setEnabled(false);
        return registration;
    }
}
