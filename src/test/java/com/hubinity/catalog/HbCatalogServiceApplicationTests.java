package com.hubinity.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.hubinity.catalog.domain.CategoryRepository;

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

    /**
     * {@code application-test.yml} also excludes {@code DataJpaRepositoriesAutoConfiguration}
     * (no DataSource here either), so no real Spring Data proxy exists for
     * {@link CategoryRepository}. {@code CategoryService} (the first real business
     * bean wired into the full context) needs one — mocked here for the same
     * "fully offline" reason as {@link #jwtDecoder}.
     */
    @MockitoBean
    private CategoryRepository categoryRepository;

    @Test
    void contextLoads() {
    }
}
