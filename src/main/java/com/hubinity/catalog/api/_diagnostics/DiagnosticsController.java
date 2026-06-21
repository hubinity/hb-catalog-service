package com.hubinity.catalog.api._diagnostics;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Throwaway controller used to validate the JWT/security wiring end-to-end.
 *
 * <p>Removed when real catalog endpoints land (features 1.5+); kept only to
 * exercise the JWT pipeline. The folder name ({@code _diagnostics}) makes the
 * removal trivial — delete this package.
 */
@RestController
@RequestMapping("/api/v1/_diagnostics")
public class DiagnosticsController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
        return Map.of(
            "authenticated", true,
            "name", authentication.getName(),
            "authorities", authorities
        );
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('admin')")
    public Map<String, Object> adminOnly() {
        return Map.of("ok", true);
    }

    /**
     * Reachable without an Authorization header — must be listed in the
     * filter chain's {@code permitAll} matchers (see {@link
     * com.hubinity.catalog.config.SecurityConfig}).
     */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        return Map.of("ok", true);
    }
}
