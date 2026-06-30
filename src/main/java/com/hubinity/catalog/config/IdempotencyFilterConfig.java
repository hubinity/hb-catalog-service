package com.hubinity.catalog.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hubinity.catalog.api.idempotency.IdempotencyFilter;
import com.hubinity.catalog.api.idempotency.IdempotencyService;

import tools.jackson.databind.ObjectMapper;

/**
 * Registers {@link IdempotencyFilter} — not as an auto-detected {@code @Component}, which
 * Spring Boot would otherwise apply to every URL by default. The four exact mutating-endpoint
 * patterns this filter cares about include a wildcard mid-path (e.g.
 * {@code /products/*}/stock/movements}), which standard servlet-mapping syntax cannot express
 * (only exact, path-prefix, or extension patterns are valid there) — so registration is
 * intentionally broad (covers the read-only movement-history `GET` too) and
 * {@code IdempotencyFilter.shouldNotFilter} does the real per-request narrowing via
 * {@code AntPathMatcher} plus a method check.
 */
@Configuration
public class IdempotencyFilterConfig {

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilter(
            IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>(
                new IdempotencyFilter(idempotencyService, objectMapper));
        registration.addUrlPatterns("/api/v1/products/*", "/api/v1/stock/*");
        return registration;
    }
}
