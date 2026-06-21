/**
 * Spring {@code @Configuration} classes for the catalog service.
 *
 * <p>Currently hosts:
 * <ul>
 *   <li>{@link com.hubinity.catalog.config.SecurityConfig} — the JWT-based
 *       Spring Security wiring (resource-server filter chain, CORS source,
 *       {@code JwtAuthenticationConverter}). Profile-aware: Swagger paths
 *       are permitted only in the {@code local} profile.</li>
 *   <li>{@link com.hubinity.catalog.config.KeycloakRealmRoleConverter} —
 *       maps Keycloak's {@code realm_access.roles} and
 *       {@code resource_access.<client>.roles} claims into Spring
 *       {@code ROLE_*} authorities (see ADR 0002).</li>
 * </ul>
 *
 * <p>Future inhabitants: {@code RabbitConfig}, {@code CacheConfig} (features
 * 1.5+).
 */
package com.hubinity.catalog.config;
