package com.hubinity.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class HbCatalogServiceApplicationTests {

    /**
     * {@code application-test.yml} excludes {@code OAuth2ResourceServerAutoConfiguration}
     * to keep this context-load test fully offline, so no real {@link JwtDecoder}
     * bean is created. {@link com.hubinity.catalog.config.SecurityConfig} still
     * wires {@code oauth2ResourceServer().jwt(...)} and therefore requires a
     * {@code JwtDecoder} in the context — this mock satisfies that requirement
     * without contacting Keycloak.
     */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
    }
}
