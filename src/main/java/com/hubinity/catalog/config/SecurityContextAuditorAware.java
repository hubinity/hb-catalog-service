package com.hubinity.catalog.config;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Provides the auditor name (Keycloak {@code preferred_username}) for Spring
 * Data JPA auditing.
 *
 * <p>Returns {@code "system"} whenever there is no authenticated principal —
 * for instance during Flyway migrations, scheduled jobs, integration tests
 * that hit the repository before authenticating, or any background thread
 * lacking a {@code SecurityContext}. The fallback keeps {@code created_by}
 * / {@code updated_by} columns non-null without forcing every entry point
 * to inject a system principal.
 */
public class SecurityContextAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.of("system");
        }
        return Optional.of(auth.getName());
    }
}
