package com.skynode;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.skynode.aircraft.entity.Aircraft;
import com.skynode.aircraft.repository.AircraftRepository;
import com.skynode.airline.entity.Airline;
import com.skynode.airline.repository.AirlineRepository;
import com.skynode.airport.entity.Airport;
import com.skynode.airport.repository.AirportRepository;
import com.skynode.flight.entity.Flight;
import com.skynode.flight.repository.FlightRepository;
import com.skynode.flight.dto.FlightInstanceRequestDto;
import com.skynode.flight.instance.service.FlightInstanceService;
import com.skynode.seat.Seat;
import com.skynode.seat.repository.SeatRepository;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.test.context.ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
    "spring.datasource.url=jdbc:h2:mem:skynode_test_security_${random.uuid};DB_CLOSE_ON_EXIT=FALSE")
class AdminAuthenticationIntegrationTest {
    @org.springframework.boot.test.context.TestConfiguration
    static class SessionTestConfiguration {
        @org.springframework.context.annotation.Bean
        static org.springframework.beans.factory.config.BeanPostProcessor isolateConsolePreferences() {
            return new org.springframework.beans.factory.config.BeanPostProcessor() {
                @Override public Object postProcessBeforeInitialization(Object bean, String name) {
                    if (bean instanceof org.springframework.boot.web.servlet.ServletRegistrationBean<?> servlet
                        && servlet.getServlet() instanceof org.h2.server.web.JakartaWebServlet) {
                        servlet.addInitParameter("properties", "target/h2-console-test-settings");
                    }
                    return bean;
                }
            };
        }

        @org.springframework.context.annotation.Bean
        SessionTracker sessionTracker() { return new SessionTracker(); }

        @org.springframework.context.annotation.Bean
        org.springframework.boot.web.servlet.ServletListenerRegistrationBean<SessionTracker> sessionListener(SessionTracker tracker) {
            return new org.springframework.boot.web.servlet.ServletListenerRegistrationBean<>(tracker);
        }
    }

    static class SessionTracker implements jakarta.servlet.http.HttpSessionListener {
        final java.util.Map<String, jakarta.servlet.http.HttpSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();
        @Override public void sessionCreated(jakarta.servlet.http.HttpSessionEvent event) {
            sessions.put(event.getSession().getId(), event.getSession());
        }
    }

    @Autowired SessionTracker sessionTracker;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired Environment environment;
    @Autowired InMemoryUserDetailsManager users;
    @Autowired PasswordEncoder passwords;
    @Autowired AirlineRepository airlines;
    @Autowired AirportRepository airports;
    @Autowired AircraftRepository aircraft;
    @Autowired SeatRepository seats;
    @Autowired FlightRepository flights;
    @Autowired FlightInstanceService scheduling;
    private final JsonMapper json = JsonMapper.builder().build();
    private static final String TEST_PASSWORD = "test-only-admin-password";

    @Test
    void anonymousUsersCannotMutateAnyAdminResource() throws Exception {
        var client = client();
        assertFalse(json.readTree(send(client, "GET", "/api/v1/auth/me", null, null).body()).get("authenticated").asBoolean());
        assertEquals(401, send(client, "GET", "/api/v1/airlines", null, null).statusCode());
        JsonNode csrf = csrf(client);
        for (String path : List.of("airports", "airlines", "aircraft?airlineId=1", "aircraft/1/seats", "flights", "flight-instances")) {
            var response = send(client, "POST", "/api/v1/" + path, "{}", csrf);
            assertEquals(401, response.statusCode(), path);
            assertEquals("Admin login required.", json.readTree(response.body()).get("message").asText());
        }
        for (String path : List.of("airlines/1", "aircraft/1?airlineId=1", "flights/1", "flight-instances/1", "flight-instances/1/status?status=SCHEDULED")) {
            assertEquals(401, send(client, "PUT", "/api/v1/" + path, "{}", csrf).statusCode(), path);
        }
        for (String path : List.of("airports/1", "airlines/1", "aircraft/1", "flights/1")) {
            assertEquals(401, send(client, "DELETE", "/api/v1/" + path, null, csrf).statusCode(), path);
        }
        assertEquals(401, send(client, "GET", "/api/v1/auth/session", null, null).statusCode());
        assertEquals(401, send(client, "GET", "/h2-console/", null, null).statusCode());
    }

    @Test
    void loginRotatesSessionAndLogoutInvalidatesIt() throws Exception {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var client = HttpClient.newBuilder().cookieHandler(cookies).build();
        var csrfResponse = send(client, "GET", "/api/v1/auth/csrf", null, null);
        String cookieHeader = csrfResponse.headers().firstValue("set-cookie").orElseThrow();
        assertTrue(cookieHeader.contains("HttpOnly"));
        assertTrue(cookieHeader.contains("SameSite=Lax"));
        assertFalse(cookieHeader.contains("; Secure")); // HTTP-only isolated test environment.
        String anonymousId = sessionId(cookies);
        assertEquals(200, login(client, "admin@skynode.test", TEST_PASSWORD, json.readTree(csrfResponse.body())).statusCode());
        String authenticatedId = sessionId(cookies);
        assertNotEquals(anonymousId, authenticatedId);
        var session = send(client, "GET", "/api/v1/auth/session", null, null);
        assertEquals(200, session.statusCode());
        assertEquals("admin@skynode.test", json.readTree(session.body()).get("email").asText());
        var me = json.readTree(send(client, "GET", "/api/v1/auth/me", null, null).body());
        assertTrue(me.get("authenticated").asBoolean());
        assertEquals("ADMIN", me.get("role").asText());
        JsonNode token = csrf(client);
        assertEquals(200, send(client, "POST", "/api/v1/airports",
            json.writeValueAsString(Map.of("name", "Authorized", "city", "Kochi", "country", "India")), token).statusCode());
        assertEquals(403, send(client, "POST", "/api/v1/auth/logout", null, null).statusCode());
        assertEquals(200, send(client, "GET", "/api/v1/auth/session", null, null).statusCode());
        assertEquals(200, send(client, "POST", "/api/v1/auth/logout", null, token).statusCode());
        assertEquals(401, send(client, "GET", "/api/v1/auth/session", null, null).statusCode());
        var replay = HttpRequest.newBuilder(uri("/api/v1/auth/session"))
            .header("Cookie", "SKYNODESESSION=" + authenticatedId).GET().build();
        assertEquals(401, HttpClient.newHttpClient().send(replay, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void incorrectCredentialsNeverAuthenticateAndErrorsStayGeneric() throws Exception {
        var client = client();
        JsonNode token = csrf(client);
        var wrongPassword = login(client, "admin@skynode.test", "incorrect-test-password", token);
        var unknownEmail = login(client, "unknown@skynode.test", "incorrect-test-password", token);
        assertEquals(401, wrongPassword.statusCode());
        assertEquals(401, unknownEmail.statusCode());
        assertEquals("Incorrect email or password.", json.readTree(wrongPassword.body()).get("message").asText());
        assertEquals(json.readTree(wrongPassword.body()).get("message"), json.readTree(unknownEmail.body()).get("message"));
        assertEquals(401, send(client, "GET", "/api/v1/auth/session", null, null).statusCode());
        assertFalse(wrongPassword.body().contains("password-hash"));
    }

    @Test
    void csrfIsRequiredForLoginAndAllWritesIncludingPublicBooking() throws Exception {
        var client = client();
        assertEquals(403, login(client, "admin@skynode.test", TEST_PASSWORD, null).statusCode());
        assertEquals(403, send(client, "POST", "/api/v1/bookings", "{}", null).statusCode());
        assertEquals(403, send(client, "PUT", "/api/v1/bookings/1/cancel", null, null).statusCode());
        assertEquals(200, login(client, "admin@skynode.test", TEST_PASSWORD, csrf(client)).statusCode());
        assertEquals(403, send(client, "POST", "/api/v1/airports", "{}", null).statusCode());
        var invalid = json.readTree("{\"headerName\":\"X-CSRF-TOKEN\",\"token\":\"invalid\"}");
        assertEquals(403, send(client, "POST", "/api/v1/airports", "{}", invalid).statusCode());
    }

    @Test
    void authenticatedNonAdminCannotPerformAdministrativeActions() throws Exception {
        String name = "viewer-" + UUID.randomUUID();
        users.createUser(User.withUsername(name).password(passwords.encode(TEST_PASSWORD)).roles("VIEWER").build());
        try {
            var client = client();
            assertEquals(200, login(client, name, TEST_PASSWORD, csrf(client)).statusCode());
            assertEquals(403, send(client, "GET", "/api/v1/auth/session", null, null).statusCode());
            assertEquals(403, send(client, "POST", "/api/v1/flights", "{}", csrf(client)).statusCode());
        } finally { users.deleteUser(name); }
    }

    @Test
    void passengerSearchBookingAndCancellationRemainPublic() throws Exception {
        String suffix = UUID.randomUUID().toString();
        var airline = airlines.saveAndFlush(new Airline("Public Test", suffix));
        var origin = airports.saveAndFlush(new Airport("Public Origin", "India", "Kochi"));
        var destination = airports.saveAndFlush(new Airport("Public Destination", "India", "Delhi"));
        var plane = aircraft.saveAndFlush(new Aircraft(airline, "A320", suffix));
        var seat = seats.saveAndFlush(new Seat(plane, "1A", "ECONOMY"));
        var flight = flights.saveAndFlush(new Flight(airline, origin, destination, plane, suffix));
        var instance = scheduling.scheduleFlightInstance(new FlightInstanceRequestDto(flight.getId(),
            "2027-08-01T09:00:00", "2027-08-01T11:00:00", "SCHEDULED"));
        var client = client();
        for (String path : List.of("airports", "aircraft", "flights", "flight-instances", "aircraft/" + plane.getId() + "/seats")) {
            assertEquals(200, send(client, "GET", "/api/v1/" + path, null, null).statusCode(), path);
        }
        String search = "/api/v1/flights/search?departureDate=2027-08-01&originAirportId=" + origin.getId();
        assertEquals(1, json.readTree(send(client, "GET", search, null, null).body()).get(0).get("availableSeatCount").asLong());
        JsonNode token = csrf(client);
        var booked = send(client, "POST", "/api/v1/bookings", json.writeValueAsString(Map.of(
            "passengerName", "Public Passenger", "flightInstanceId", instance.getId(), "seatId", seat.getId())), token);
        assertEquals(200, booked.statusCode());
        var booking = json.readTree(booked.body());
        assertEquals(200, send(client, "GET", "/api/v1/bookings/pnr/" + booking.get("pnr").asText(), null, null).statusCode());
        assertEquals(0, json.readTree(send(client, "GET", search, null, null).body()).get(0).get("availableSeatCount").asLong());
        assertEquals(200, send(client, "PUT", "/api/v1/bookings/" + booking.get("id").asLong() + "/cancel", null, token).statusCode());
        assertEquals(1, json.readTree(send(client, "GET", search, null, null).body()).get(0).get("availableSeatCount").asLong());
        assertEquals(401, send(client, "GET", "/api/v1/auth/session", null, null).statusCode());
    }

    @Test
    void missingOrInvalidAdminConfigurationFailsClosed() {
        var config = new com.skynode.security.AdminSecurityConfig();
        assertThrows(IllegalArgumentException.class, () -> config.adminUsers("", ""));
        assertThrows(IllegalArgumentException.class, () -> config.adminUsers("skynode@admin.com", "plaintext"));
    }

    @Test
    void invalidSessionCannotAccessManagementOrReuseCsrf() throws Exception {
        var request = HttpRequest.newBuilder(uri("/api/v1/auth/me"))
            .header("Cookie", "SKYNODESESSION=invalid-session").GET().build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertFalse(json.readTree(response.body()).get("authenticated").asBoolean());
        var anonymous = client();
        assertEquals(401, send(anonymous, "POST", "/api/v1/flights", "{}", csrf(anonymous)).statusCode());
        assertEquals(403, send(anonymous, "POST", "/api/v1/flights", "{}", null).statusCode());
    }

    @Test
    void idleSessionExpiryRemovesAdministrativeAccess() throws Exception {
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var client = HttpClient.newBuilder().cookieHandler(cookies).build();
        var token = csrf(client);
        var session = sessionTracker.sessions.get(sessionId(cookies));
        assertNotNull(session);
        assertEquals(1800, session.getMaxInactiveInterval());
        assertEquals(200, login(client, "admin@skynode.test", TEST_PASSWORD, token).statusCode());
        token = csrf(client);
        session.setMaxInactiveInterval(1);
        Thread.sleep(1500);
        assertFalse(json.readTree(send(client, "GET", "/api/v1/auth/me", null, null).body()).get("authenticated").asBoolean());
        assertEquals(403, send(client, "POST", "/api/v1/airports", "{}", token).statusCode());
        assertEquals(401, send(client, "POST", "/api/v1/airports", "{}", csrf(client)).statusCode());
    }

    @Test
    void localConsoleRequiresAdminAndDoesNotWeakenApiHeadersOrCsrf() throws Exception {
        var client = client();
        for (String path : List.of("/h2-console", "/h2-console/", "/h2-console/login.jsp", "/h2-console/stylesheet.css")) {
            assertEquals(401, send(client, "GET", path, null, null).statusCode(), path);
        }
        assertTrue(environment.getProperty("spring.h2.console.enabled", Boolean.class, false));
        assertFalse(environment.getProperty("spring.h2.console.settings.web-allow-others", Boolean.class, true));
        assertEquals(200, login(client, "admin@skynode.test", TEST_PASSWORD, csrf(client)).statusCode());
        var console = send(client, "GET", "/h2-console/", null, null);
        assertEquals(200, console.statusCode());
        assertEquals("SAMEORIGIN", console.headers().firstValue("X-Frame-Options").orElseThrow());
        assertEquals(200, send(client, "GET", "/h2-console/stylesheet.css", null, null).statusCode());
        var sessionMatch = java.util.regex.Pattern.compile("jsessionid=([a-zA-Z0-9]+)").matcher(console.body());
        assertTrue(sessionMatch.find(), "H2 should issue its console navigation session");
        String consoleSession = sessionMatch.group(1);
        var form = send(client, "GET", "/h2-console/login.jsp?jsessionid=" + consoleSession, null, null);
        assertEquals(200, form.statusCode());
        assertTrue(form.body().contains("jdbc:h2:"));
        String jdbcUrl;
        try (var connection = dataSource.getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
        }
        assertTrue(jdbcUrl.startsWith("jdbc:h2:mem:skynode_test_"));
        String connection = "driver=org.h2.Driver&url=" + URLEncoder.encode(
            jdbcUrl + ";IFEXISTS=TRUE", StandardCharsets.UTF_8) + "&user=sa&password=&language=en";
        var connect = HttpRequest.newBuilder(uri("/h2-console/login.do?jsessionid=" + consoleSession))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(connection)).build();
        var connected = client.send(connect, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, connected.statusCode());
        assertTrue(connected.body().contains("<frameset"), "Console should connect to the existing isolated test database");
        assertEquals(200, send(client, "GET", "/h2-console/query.jsp?jsessionid=" + consoleSession, null, null).statusCode());
        var catalog = send(client, "GET", "/api/v1/airports", null, null);
        assertEquals("DENY", catalog.headers().firstValue("X-Frame-Options").orElseThrow());
        assertEquals(403, send(client, "POST", "/api/v1/airports", "{}", null).statusCode());
        assertEquals(200, send(client, "POST", "/api/v1/auth/logout", null, csrf(client)).statusCode());
        assertEquals(401, send(client, "GET", "/h2-console/", null, null).statusCode());
        assertEquals(401, send(client, "GET", "/h2-console/frame.jsp?jsessionid=" + consoleSession, null, null).statusCode());
    }

    @Test
    void hostOnlyCookieWorksAcrossPortsButNotDifferentHostnames() throws Exception {
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var client = HttpClient.newBuilder().cookieHandler(cookies).build();
        URI tokenUri = URI.create(uri("/api/v1/auth/csrf").toString().replace("localhost", "127.0.0.1"));
        var tokenResponse = client.send(HttpRequest.newBuilder(tokenUri).GET().build(), HttpResponse.BodyHandlers.ofString());
        var token = json.readTree(tokenResponse.body());
        String cookieHeader = tokenResponse.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookieHeader.contains("Path=/"));
        assertTrue(cookieHeader.contains("HttpOnly"));
        assertTrue(cookieHeader.contains("SameSite=Lax"));
        assertFalse(cookieHeader.toLowerCase().contains("domain="));
        var login = HttpRequest.newBuilder(URI.create(uri("/api/v1/auth/login").toString().replace("localhost", "127.0.0.1")))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header(token.get("headerName").asText(), token.get("token").asText())
            .POST(HttpRequest.BodyPublishers.ofString("email=admin%40skynode.test&password=" + TEST_PASSWORD)).build();
        assertEquals(200, client.send(login, HttpResponse.BodyHandlers.ofString()).statusCode());
        var console = HttpRequest.newBuilder(URI.create(uri("/h2-console/").toString().replace("localhost", "127.0.0.1"))).GET().build();
        assertEquals(200, client.send(console, HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(401, send(client, "GET", "/h2-console/", null, null).statusCode());
        assertFalse(cookies.get(URI.create("http://127.0.0.1:5173/api/v1/auth/me"), Map.of()).get("Cookie").isEmpty());
    }

    @Test
    void browserConsoleNavigationUsesFixedLoginAndReturnDestinations() throws Exception {
        var client = client();
        var navigation = HttpRequest.newBuilder(uri("/h2-console?returnTo=https://example.invalid"))
            .header("Accept", "text/html").GET().build();
        var redirect = client.send(navigation, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, redirect.statusCode());
        assertEquals("http://localhost:5173/#/admin/login?returnTo=h2-console",
            redirect.headers().firstValue("Location").orElseThrow());
        assertEquals(401, send(client, "GET", "/h2-console", null, null).statusCode());
        var apiRequest = HttpRequest.newBuilder(uri("/api/v1/airlines"))
            .header("Accept", "text/html").GET().build();
        var apiResponse = client.send(apiRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, apiResponse.statusCode());
        assertFalse(apiResponse.headers().firstValue("Location").isPresent());
        assertEquals(200, login(client, "admin@skynode.test", TEST_PASSWORD, csrf(client)).statusCode());
        var me = json.readTree(send(client, "GET", "/api/v1/auth/me", null, null).body());
        assertTrue(me.get("authenticated").asBoolean());
        assertEquals("ADMIN", me.get("role").asText());
        var authenticated = client.send(navigation, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, authenticated.statusCode()); // H2 canonicalizes its servlet URL with a trailing slash.
        assertEquals("/h2-console/", URI.create(authenticated.headers().firstValue("Location").orElseThrow()).getPath());
        var console = HttpRequest.newBuilder(uri("/h2-console/")).header("Accept", "text/html").GET().build();
        assertEquals(200, client.send(console, HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(200, send(client, "POST", "/api/v1/auth/logout", null, csrf(client)).statusCode());
        var afterLogout = client.send(navigation, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, afterLogout.statusCode());
        assertEquals("http://localhost:5173/#/admin/login?returnTo=h2-console",
            afterLogout.headers().firstValue("Location").orElseThrow());
    }

    @Test
    void publicMockPaymentRequiresOtpAndReusesConfirmationOnRepeat() throws Exception {
        String suffix = UUID.randomUUID().toString();
        var airline = airlines.saveAndFlush(new Airline("Demo Pay", suffix));
        var origin = airports.saveAndFlush(new Airport("Payment Origin", "India", "Kochi"));
        var destination = airports.saveAndFlush(new Airport("Payment Destination", "India", "Delhi"));
        var plane = aircraft.saveAndFlush(new Aircraft(airline, "A320", suffix));
        var seat = seats.saveAndFlush(new Seat(plane, "1A", "ECONOMY"));
        var flight = flights.saveAndFlush(new Flight(airline, origin, destination, plane, suffix));
        var instance = scheduling.scheduleFlightInstance(new FlightInstanceRequestDto(flight.getId(),
            "2027-08-02T09:00:00", "2027-08-02T11:00:00", "SCHEDULED"));
        var client = client();
        JsonNode token = csrf(client);
        var body = new java.util.HashMap<String, Object>();
        body.put("fullName", "Demo Passenger");
        body.put("email", "demo@example.test");
        body.put("phone", "9876543210");
        body.put("age", 25);
        body.put("gender", "Other");
        body.put("flightInstanceId", instance.getId());
        body.put("seatId", seat.getId());
        String path = "/api/v1/mock-payments/start";
        assertEquals(403, send(client, "POST", path, json.writeValueAsString(body), null).statusCode());
        assertEquals(400, send(client, "POST", path, json.writeValueAsString(Map.of("fullName", "A")), token).statusCode());
        var start = send(client, "POST", path, json.writeValueAsString(body), token);
        assertEquals(200, start.statusCode());
        var payment = json.readTree(start.body());
        assertEquals(400, send(client, "POST", "/api/v1/mock-payments/verify", json.writeValueAsString(
            Map.of("paymentId", payment.get("paymentId").asText(), "otp", "123")), token).statusCode());
        String verify = json.writeValueAsString(Map.of("paymentId", payment.get("paymentId").asText(), "otp", "1234"));
        var first = send(client, "POST", "/api/v1/mock-payments/verify", verify, token);
        assertEquals(200, first.statusCode());
        var confirmed = json.readTree(first.body()).get("booking");
        assertEquals("PAID_DEMO", confirmed.get("paymentStatus").asText());
        assertEquals("demo@example.test", confirmed.get("passengerEmail").asText());
        assertEquals(4999, confirmed.get("totalAmount").asInt());
        var repeat = json.readTree(send(client, "POST", "/api/v1/mock-payments/verify", verify, token).body()).get("booking");
        assertEquals(confirmed.get("pnr").asText(), repeat.get("pnr").asText());
        var lookup = json.readTree(send(client, "GET", "/api/v1/bookings/pnr/" + confirmed.get("pnr").asText(), null, null).body());
        assertEquals("PAID_DEMO", lookup.get("paymentStatus").asText());
        assertEquals(200, send(client, "PUT", "/api/v1/bookings/" + confirmed.get("id").asLong() + "/cancel", null, token).statusCode());
    }

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + environment.getRequiredProperty("local.server.port") + path);
    }

    private JsonNode csrf(HttpClient client) throws Exception {
        var response = send(client, "GET", "/api/v1/auth/csrf", null, null);
        assertEquals(200, response.statusCode());
        return json.readTree(response.body());
    }

    private HttpResponse<String> login(HttpClient client, String email, String password, JsonNode token) throws Exception {
        var builder = HttpRequest.newBuilder(uri("/api/v1/auth/login"))
            .header("Content-Type", "application/x-www-form-urlencoded");
        if (token != null) builder.header(token.get("headerName").asText(), token.get("token").asText());
        return client.send(builder.POST(HttpRequest.BodyPublishers.ofString("email="
            + URLEncoder.encode(email, StandardCharsets.UTF_8) + "&password="
            + URLEncoder.encode(password, StandardCharsets.UTF_8))).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> send(HttpClient client, String method, String path, String body, JsonNode token) throws Exception {
        var builder = HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json");
        if (token != null) builder.header(token.get("headerName").asText(), token.get("token").asText());
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String sessionId(CookieManager cookies) {
        return cookies.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equals("SKYNODESESSION"))
            .findFirst().orElseThrow().getValue();
    }
}
