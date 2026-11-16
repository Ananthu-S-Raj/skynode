package com.skynode;

import java.sql.DriverManager;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.*;

class SeatSchemaSafetyTest {
    @Test
    void duplicateLegacySeatsStopStartupBeforeHibernateChangesSchema() throws Exception {
        String url = "jdbc:h2:mem:skynode_test_legacy_" + UUID.randomUUID();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE seats (id BIGINT PRIMARY KEY, aircraft_id BIGINT, seat_number VARCHAR(255))");
            statement.execute("INSERT INTO seats VALUES (1, 1, '1A'), (2, 1, '1A')");
            var app = new SpringApplication(SkyNodeApplication.class);
            Exception failure = assertThrows(Exception.class, () -> {
                try (var ignored = app.run("--spring.main.web-application-type=none",
                        "--spring.datasource.url=" + url, "--spring.jpa.hibernate.ddl-auto=update")) {
                    fail("Duplicate seats must prevent schema update");
                }
            });
            Throwable cause = failure;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            assertTrue(cause.getMessage().contains("Duplicate aircraft seat numbers"));
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM seats")) {
                assertTrue(rows.next());
                assertEquals(2, rows.getInt(1));
            }
            try (var tables = connection.getMetaData().getTables(null, "PUBLIC", "BOOKINGS", null)) {
                assertFalse(tables.next());
            }
        }
    }
    @Test
    void cleanLegacySeatsGainConstraintWithoutLosingRecords() throws Exception {
        String url = "jdbc:h2:mem:skynode_test_migration_" + UUID.randomUUID();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            try (var ignored = new SpringApplication(SkyNodeApplication.class).run(
                    "--spring.main.web-application-type=none", "--spring.datasource.url=" + url,
                    "--spring.jpa.hibernate.ddl-auto=create")) {
                statement.execute("INSERT INTO airlines(id,name,code) VALUES (1,'Legacy Airline','LEG')");
                statement.execute("INSERT INTO aircraft(id,airline_id,model,registration_number) VALUES (1,1,'A320','LEG-1')");
                statement.execute("INSERT INTO seats(id,aircraft_id,seat_number) VALUES (1,1,'1A')");
            }
            statement.execute("ALTER TABLE seats DROP CONSTRAINT uk_aircraft_seat_number");
            try (var ignored = new SpringApplication(SkyNodeApplication.class).run(
                    "--spring.main.web-application-type=none", "--spring.datasource.url=" + url,
                    "--spring.jpa.hibernate.ddl-auto=update")) {
                java.sql.SQLException duplicate = assertThrows(java.sql.SQLException.class,
                    () -> statement.execute("INSERT INTO seats(id,aircraft_id,seat_number) VALUES (2,1,'1A')"));
                assertEquals("23505", duplicate.getSQLState());
                try (var rows = statement.executeQuery("SELECT COUNT(*) FROM seats")) {
                    assertTrue(rows.next());
                    assertEquals(1, rows.getInt(1));
                }
            }
        }
    }
}
