package com.skynode;

import java.net.URI;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
    "spring.datasource.url=jdbc:h2:mem:skynode_test_api_${random.uuid};DB_CLOSE_ON_EXIT=FALSE")
class AdminBookingApiIntegrationTest {
    @Autowired
    Environment environment;

    private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    private JsonNode csrf;
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void authenticateAdmin() throws Exception {
        String base = "http://localhost:" + environment.getRequiredProperty("local.server.port");
        csrf = json.readTree(client.send(HttpRequest.newBuilder(URI.create(base + "/api/v1/auth/csrf")).GET().build(),
            HttpResponse.BodyHandlers.ofString()).body());
        var login = HttpRequest.newBuilder(URI.create(base + "/api/v1/auth/login"))
            .header(csrf.get("headerName").asText(), csrf.get("token").asText())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("email=admin%40skynode.test&password=test-only-admin-password")).build();
        assertEquals(200, client.send(login, HttpResponse.BodyHandlers.ofString()).statusCode());
        csrf = json.readTree(client.send(HttpRequest.newBuilder(URI.create(base + "/api/v1/auth/csrf")).GET().build(),
            HttpResponse.BodyHandlers.ofString()).body());
    }

    @Test
    void consoleIsUnavailableOutsideLocalEvenForAdmin() throws Exception {
        assertFalse(environment.getProperty("spring.h2.console.enabled", Boolean.class, true));
        request("GET", "/h2-console", null, 403);
        request("GET", "/h2-console/login.jsp", null, 403);
    }

    @Test
    void adminConfigurationAndPassengerBookingWorkflow() throws Exception {
        long airline = createAirline();
        long origin = request("POST", "/api/v1/airports",
            Map.of("name", "Origin", "country", "India", "city", "Kochi"), 200).get("id").asLong();
        long destination = request("POST", "/api/v1/airports",
            Map.of("name", "Destination", "country", "India", "city", "Delhi"), 200).get("id").asLong();
        long plane = createAircraft(airline);
        long otherPlane = createAircraft(airline);
        String seatsPath = "/api/v1/aircraft/" + plane + "/seats";
        long seat = request("POST", seatsPath, Map.of("seatNumber", "1A", "seatClass", "ECONOMY"), 200)
            .get("id").asLong();
        request("POST", seatsPath, Map.of("seatNumber", "1A", "seatClass", "ECONOMY"), 409);
        request("POST", "/api/v1/aircraft/" + otherPlane + "/seats", Map.of("seatNumber", "1A"), 200);
        assertEquals(1, request("GET", seatsPath, null, 200).size());
        assertEquals(seat, request("GET", seatsPath + "/1A", null, 200).get("id").asLong());
        request("GET", seatsPath + "/99Z", null, 404);

        Map<String, Object> flight = Map.of("airlineId", airline, "aircraftId", plane,
            "originAirportId", origin, "destinationAirportId", destination, "flightNumber", "API-" + UUID.randomUUID());
        long flightId = request("POST", "/api/v1/flights", flight, 200).get("id").asLong();
        String instancesPath = "/api/v1/flight-instances";
        Map<String, Object> times = Map.of("departureDateTime", "2027-05-15T09:00:00",
            "arrivalDateTime", "2027-05-15T11:00:00");
        long instance = request("POST", instancesPath + "?flightId=" + flightId, times, 200).get("id").asLong();
        assertEquals("SCHEDULED", request("GET", instancesPath + "/" + instance, null, 200).get("status").asText());
        assertTrue(request("GET", instancesPath, null, 200).size() >= 1);
        assertEquals(1, request("GET", instancesPath + "/flight/" + flightId, null, 200).size());
        request("PUT", instancesPath + "/" + instance, Map.of("arrivalDateTime", "2027-05-15T12:00:00"), 200);
        request("PUT", instancesPath + "/" + instance + "/status?status=DELAYED", null, 200);
        request("PUT", instancesPath + "/" + instance + "/status?status=UNKNOWN", null, 400);

        request("POST", instancesPath, Map.of("flightId", flightId,
            "departureDateTime", "2027-05-16T09:00:00", "arrivalDateTime", "2027-05-16T11:00:00"), 200);
        request("POST", instancesPath + "?flightId=" + flightId, Map.of("flightId", Long.MAX_VALUE,
            "departureDateTime", "2027-05-16T09:00:00", "arrivalDateTime", "2027-05-16T11:00:00"), 400);
        request("PUT", instancesPath + "/" + instance, Map.of("arrivalDateTime", "2027-05-15T08:00:00"), 400);
        request("PUT", instancesPath + "/" + instance, Map.of("flightId", Long.MAX_VALUE), 400);

        request("PUT", "/api/v1/flights/" + flightId, Map.of("airlineId", airline, "aircraftId", otherPlane,
            "originAirportId", origin, "destinationAirportId", destination, "flightNumber", "REASSIGNED"), 409);
        request("PUT", "/api/v1/flights/" + flightId, flight, 200);
        String search = "/api/v1/flights/search?departureDate=2027-05-15&originAirportId=" + origin;
        assertEquals(1L, request("GET", search, null, 200).get(0).get("availableSeatCount").asLong());
        assertEquals(0, request("GET", "/api/v1/flights/search?departureDate=2027-05-15&originAirportId=9223372036854775807", null, 200).size());
        assertEquals(1, request("GET", "/api/v1/flights/search?departureDate=2027-05-15&destinationAirportId=" + destination, null, 200).size());

        Map<String, Object> booking = Map.of("passengerName", "API Passenger " + UUID.randomUUID(),
            "flightInstanceId", instance, "seatId", seat);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        JsonNode winner;
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<HttpResponse<String>> attempt = () -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return send("POST", "/api/v1/bookings", booking);
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var a = first.get(15, TimeUnit.SECONDS);
            var b = second.get(15, TimeUnit.SECONDS);
            assertTrue(a.statusCode() == 200 && b.statusCode() == 409 || a.statusCode() == 409 && b.statusCode() == 200);
            winner = json.readTree(a.statusCode() == 200 ? a.body() : b.body());
        }
        String pnr = winner.get("pnr").asText();
        assertTrue(pnr.matches("[A-F0-9]{8}"));
        long bookingId = winner.get("id").asLong();
        assertEquals(pnr, request("GET", "/api/v1/bookings/" + bookingId, null, 200).get("pnr").asText());
        request("GET", "/api/v1/bookings/pnr/" + pnr, null, 200);
        assertEquals(0L, request("GET", search, null, 200).get(0).get("availableSeatCount").asLong());
        request("PUT", "/api/v1/bookings/" + bookingId + "/cancel", null, 200);
        assertEquals(1L, request("GET", search, null, 200).get(0).get("availableSeatCount").asLong());
        request("POST", "/api/v1/bookings", booking, 200);
        request("PUT", "/api/v1/bookings/" + bookingId + "/cancel", null, 409);
        assertEquals(0L, request("GET", search, null, 200).get(0).get("availableSeatCount").asLong());
    }

    @Test
    void invalidSchedulingAndSeatRequestsReturn400() throws Exception {
        long plane = createAircraft(createAirline());
        request("POST", "/api/v1/aircraft/" + plane + "/seats", Map.of("seatNumber", ""), 400);
        request("POST", "/api/v1/flight-instances", Map.of(), 400);
        request("POST", "/api/v1/flight-instances", Map.of("flightId", 1,
            "departureDateTime", "2027-02-30T25:00:00", "arrivalDateTime", "2027-03-01T09:00:00"), 400);
        request("POST", "/api/v1/flight-instances", Map.of("flightId", 1,
            "departureDateTime", "2027-05-15T09:00:00", "arrivalDateTime", "2027-05-15T08:00:00"), 400);
        request("POST", "/api/v1/flight-instances", Map.of("flightId", 1, "status", "UNKNOWN",
            "departureDateTime", "2027-05-15T09:00:00", "arrivalDateTime", "2027-05-15T10:00:00"), 400);
        request("GET", "/api/v1/flight-instances/9223372036854775807", null, 404);
    }

    @Test
    void concurrentSeatConfigurationHasOneWinner() throws Exception {
        long plane = createAircraft(createAirline());
        String path = "/api/v1/aircraft/" + plane + "/seats";
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> add = () -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return send("POST", path, Map.of("seatNumber", "1A")).statusCode();
            };
            var first = executor.submit(add);
            var second = executor.submit(add);
            start.countDown();
            int a = first.get(15, TimeUnit.SECONDS);
            int b = second.get(15, TimeUnit.SECONDS);
            assertTrue(a == 200 && b == 409 || a == 409 && b == 200);
        }
        assertEquals(1, request("GET", path, null, 200).size());
    }

    private long createAirline() throws Exception {
        return request("POST", "/api/v1/airlines", Map.of("name", "API Airline", "code", UUID.randomUUID().toString()), 201)
            .get("id").asLong();
    }

    private long createAircraft(long airline) throws Exception {
        return request("POST", "/api/v1/aircraft?airlineId=" + airline,
            Map.of("model", "A320", "registrationNumber", UUID.randomUUID().toString()), 200).get("id").asLong();
    }

    private JsonNode request(String method, String path, Object body, int status) throws Exception {
        var response = send(method, path, body);
        assertEquals(status, response.statusCode(), response.body());
        return response.body().isEmpty() ? null : json.readTree(response.body());
    }

    private HttpResponse<String> send(String method, String path, Object body) throws Exception {
        String base = "http://localhost:" + environment.getRequiredProperty("local.server.port");
        var builder = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json");
        if (!method.equals("GET")) builder.header(csrf.get("headerName").asText(), csrf.get("token").asText());
        var request = builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
