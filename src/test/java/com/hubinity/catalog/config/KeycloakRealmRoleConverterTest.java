package com.hubinity.catalog.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Pure-JUnit tests for {@link KeycloakRealmRoleConverter}. No Spring context.
 */
class KeycloakRealmRoleConverterTest {

    private static final String CLIENT_ID = "hb-catalog-service";

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter(CLIENT_ID);

    @Test
    void mapsRealmAccessRolesToRolePrefixedAuthorities() {
        Jwt jwt = jwtWithClaims(Map.of(
            "realm_access", Map.of("roles", List.of("admin"))
        ));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_admin");
    }

    @Test
    void mergesRealmAndResourceAccessRolesAndDedupes() {
        Jwt jwt = jwtWithClaims(Map.of(
            "realm_access", Map.of("roles", List.of("admin", "tecnico")),
            "resource_access", Map.of(
                CLIENT_ID, Map.of("roles", List.of("admin", "catalog-writer"))
            )
        ));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_admin", "ROLE_tecnico", "ROLE_catalog-writer");
    }

    @Test
    void returnsEmptyAuthoritiesWhenNoRoleClaims() {
        Jwt jwt = jwtWithClaims(Map.of(
            "preferred_username", "alice"
        ));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void returnsEmptyAuthoritiesWhenRealmAccessRolesIsNull() {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", null);

        Jwt jwt = jwtWithClaims(Map.of(
            "realm_access", realmAccess
        ));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    private static Jwt jwtWithClaims(Map<String, Object> claims) {
        // Jwt requires at least one header claim; "alg" is the conventional one.
        Map<String, Object> headers = Map.of("alg", "RS256");
        Map<String, Object> withSub = new HashMap<>(claims);
        withSub.putIfAbsent("sub", "test-subject");
        return new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(300),
            headers,
            withSub
        );
    }
}
