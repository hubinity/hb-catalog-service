package com.hubinity.catalog.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Converts a Keycloak-issued JWT into Spring Security authorities.
 *
 * <p>Keycloak emits roles in two distinct claims:
 * <ul>
 *   <li>{@code realm_access.roles} — roles assigned at the realm level
 *       (e.g. {@code admin}, {@code tecnico}, {@code atendente}).</li>
 *   <li>{@code resource_access.<client-id>.roles} — roles assigned at the
 *       client-mapper level for the per-service OIDC client.</li>
 * </ul>
 *
 * <p>This converter merges both claims (deduped) and prefixes each role name
 * with {@code ROLE_} so Spring's {@code hasRole(...)} expressions work as
 * expected. The target client id is configurable via the
 * {@code app.security.keycloak.client-id} property and defaults to
 * {@code hb-catalog-service}.
 *
 * <p>Rationale and trade-offs are captured in ADR 0002.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Logger log = LoggerFactory.getLogger(KeycloakRealmRoleConverter.class);

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_KEY = "roles";

    private final String clientId;

    public KeycloakRealmRoleConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        roles.addAll(extractRealmRoles(jwt));
        roles.addAll(extractClientRoles(jwt));

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role));
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolved {} Keycloak authorities: {}", authorities.size(), roles);
        }
        return authorities;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            return List.of();
        }
        Object rolesObj = realmAccess.get(ROLES_KEY);
        if (rolesObj instanceof List<?> rolesList) {
            return (List<String>) rolesList;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess == null) {
            return List.of();
        }
        Object clientEntry = resourceAccess.get(clientId);
        if (!(clientEntry instanceof Map<?, ?> clientMap)) {
            return List.of();
        }
        Object rolesObj = clientMap.get(ROLES_KEY);
        if (rolesObj instanceof List<?> rolesList) {
            return (List<String>) rolesList;
        }
        return List.of();
    }
}
