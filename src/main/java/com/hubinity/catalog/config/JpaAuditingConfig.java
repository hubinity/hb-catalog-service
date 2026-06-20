package com.hubinity.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing.
 *
 * <p>Populates {@code @CreatedDate}, {@code @LastModifiedDate},
 * {@code @CreatedBy} and {@code @LastModifiedBy} on every entity annotated
 * with {@code @EntityListeners(AuditingEntityListener.class)}. The auditor
 * username is resolved via {@link SecurityContextAuditorAware}.
 *
 * <p>Disabled under the {@code test} profile: that profile turns off Hibernate
 * and the JPA repositories autoconfig to keep the bootstrap
 * {@code contextLoads} smoke test offline, which leaves Spring Data with no
 * JPA metamodel for {@code @EnableJpaAuditing} to attach to. Full JPA wiring
 * is exercised by the {@code @Tag("integration")} Testcontainers suites.
 */
@Configuration
@Profile("!test")
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return new SecurityContextAuditorAware();
    }
}
