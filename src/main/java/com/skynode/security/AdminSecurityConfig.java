package com.skynode.security;

import java.util.Map;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import tools.jackson.databind.json.JsonMapper;
import com.skynode.ErrorResponse;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AdminSecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public InMemoryUserDetailsManager adminUsers(@Value("${skynode.admin.email}") String email,
                                                @Value("${skynode.admin.password-hash}") String hash) {
        if (email.isBlank() || !email.contains("@") || !hash.matches("\\$2[aby]\\$(1[0-9]|2[0-9]|3[01])\\$[./A-Za-z0-9]{53}")) {
            throw new IllegalArgumentException("Configure SKYNODE_ADMIN_EMAIL and a BCrypt SKYNODE_ADMIN_PASSWORD_HASH (cost 10 or higher)");
        }
        return new InMemoryUserDetailsManager(User.withUsername(email).password(hash).roles("ADMIN").build());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper json) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/v1/auth/csrf", "/api/v1/auth/me", "/api/v1/auth/login", "/health").permitAll()
                .requestMatchers("/api/v1/auth/session").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api-docs", "/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                // Public catalog reads are needed by passenger search and seat selection.
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/airports", "/api/v1/airports/*",
                    "/api/v1/aircraft", "/api/v1/aircraft/*",
                    "/api/v1/aircraft/*/seats", "/api/v1/aircraft/*/seats/*",
                    "/api/v1/flights", "/api/v1/flights/*",
                    "/api/v1/flight-instances", "/api/v1/flight-instances/*",
                    "/api/v1/flight-instances/flight/*",
                    "/api/v1/bookings/*", "/api/v1/bookings/pnr/*",
                    "/api/v1/bookings/passenger/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/bookings",
                    "/api/v1/mock-payments/start", "/api/v1/mock-payments/verify").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/*/cancel").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/airlines", "/api/v1/airlines/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/airports", "/api/v1/airlines",
                    "/api/v1/aircraft", "/api/v1/aircraft/*/seats", "/api/v1/flights",
                    "/api/v1/flight-instances").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/airlines/*", "/api/v1/aircraft/*",
                    "/api/v1/flights/*", "/api/v1/flight-instances/*",
                    "/api/v1/flight-instances/*/status").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/airports/*", "/api/v1/airlines/*",
                    "/api/v1/aircraft/*", "/api/v1/flights/*").hasRole("ADMIN")
                .anyRequest().denyAll())
            .formLogin(form -> form
                .loginPage("/api/v1/auth/login")
                .loginProcessingUrl("/api/v1/auth/login")
                .usernameParameter("email")
                .successHandler((request, response, authentication) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    json.writeValue(response.getOutputStream(), Map.of("authenticated", true, "email", authentication.getName()));
                })
                .failureHandler((request, response, failure) ->
                    writeError(json, request, response, HttpStatus.UNAUTHORIZED, "Incorrect email or password.")))
            .logout(logout -> logout
                .logoutUrl("/api/v1/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("SKYNODESESSION")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    json.writeValue(response.getOutputStream(), Map.of("authenticated", false));
                }))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, failure) ->
                    writeError(json, request, response, HttpStatus.UNAUTHORIZED, "Admin login required."))
                .accessDeniedHandler((request, response, failure) ->
                    writeError(json, request, response, HttpStatus.FORBIDDEN,
                        failure instanceof CsrfException ? "Your security token expired or is missing. Refresh and try again."
                            : "You do not have permission to perform this action.")))
            .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
            .requestCache(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf.csrfTokenRepository(new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository()));
        return http.build();
    }

    @Bean
    @Profile("local")
    @Order(1)
    public SecurityFilterChain localH2Console(HttpSecurity http, JsonMapper json) throws Exception {
        // Only this development console chain permits its own forms and same-origin frames.
        http.securityMatcher(PathRequest.toH2Console())
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .requestCache(AbstractHttpConfigurer::disable)
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, failure) -> {
                    String path = request.getRequestURI().substring(request.getContextPath().length());
                    boolean browserNavigation = request.getMethod().equals("GET")
                        && (path.equals("/h2-console") || path.equals("/h2-console/"))
                        && request.getHeader("Accept") != null
                        && request.getHeader("Accept").contains(MediaType.TEXT_HTML_VALUE);
                    if (browserNavigation) {
                        // Both destinations are fixed: request parameters can never choose a redirect host.
                        response.sendRedirect("http://localhost:5173/#/admin/login?returnTo=h2-console");
                    } else {
                        writeError(json, request, response, HttpStatus.UNAUTHORIZED, "Admin login required.");
                    }
                })
                .accessDeniedHandler((request, response, failure) ->
                    writeError(json, request, response, HttpStatus.FORBIDDEN, "You do not have permission to perform this action.")));
        return http.build();
    }

    private void writeError(JsonMapper json, HttpServletRequest request, HttpServletResponse response,
                            HttpStatus status, String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), new ErrorResponse(status.value(), status.getReasonPhrase(), message,
            "uri=" + request.getRequestURI()));
    }
}
