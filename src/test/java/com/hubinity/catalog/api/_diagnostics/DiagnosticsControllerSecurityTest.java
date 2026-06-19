package com.hubinity.catalog.api._diagnostics;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hubinity.catalog.config.SecurityConfig;

/**
 * Web-layer security slice test for {@link DiagnosticsController}.
 *
 * <p>Loads {@link SecurityConfig} so the real {@code SecurityFilterChain},
 * CORS config, and JWT-to-authority conversion run during the test. A
 * mocked {@link JwtDecoder} satisfies the OAuth2 Resource Server
 * autoconfiguration without needing a live Keycloak.
 *
 * <p>The {@link com.hubinity.catalog.config.KeycloakRealmRoleConverter}
 * itself is exercised by the dedicated pure-JUnit
 * {@code KeycloakRealmRoleConverterTest}; here we only need the filter
 * chain wiring to be live.
 */
@WebMvcTest(DiagnosticsController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    // application.yml sets spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}, so without
    // an override the local-only swagger chain would be registered and clash with the
    // catch-all default chain. Clearing the active profile forces the production chain only.
    "spring.profiles.active=",
    "app.security.keycloak.client-id=hb-catalog-service",
    "app.cors.allowed-origins=http://localhost:4200"
})
class DiagnosticsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Satisfies the OAuth2 Resource Server autoconfiguration. The bean is
     * never actually invoked because every authenticated request below uses
     * {@code SecurityMockMvcRequestPostProcessors.jwt()} which short-circuits
     * decoding.
     */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getPublic_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/_diagnostics/public"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"ok\":true}"));
    }

    @Test
    void getMe_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/_diagnostics/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_withJwt_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/_diagnostics/me").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"authenticated\":true")));
    }

    @Test
    void getAdminOnly_userRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/_diagnostics/admin-only")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_tecnico"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getAdminOnly_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/_diagnostics/admin-only")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"ok\":true}"));
    }
}
