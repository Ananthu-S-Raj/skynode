package com.skynode.security;

import java.security.Principal;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AdminAuthController {
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        boolean admin = authentication != null && authentication.isAuthenticated()
            && authentication.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));
        return admin ? Map.of("authenticated", true, "email", authentication.getName(), "role", "ADMIN")
            : Map.of("authenticated", false);
    }

    // Retained for clients using the earlier authenticated-only endpoint.
    @GetMapping("/session")
    public Map<String, Object> session(Principal principal) {
        return Map.of("authenticated", true, "email", principal.getName(), "role", "ADMIN");
    }
}
