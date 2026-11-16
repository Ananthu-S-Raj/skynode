package com.skynode.config;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeatSchemaSafetyConfig {
    @Bean
    public HibernatePropertiesCustomizer checkExistingSeats(DataSource dataSource) {
        // Run before Hibernate DDL: never attempt a unique constraint over duplicate records.
        return properties -> {
            try (var connection = dataSource.getConnection();
                 var tables = connection.getMetaData().getTables(null, connection.getSchema(), "SEATS", null)) {
                if (!tables.next()) {
                    return;
                }
                try (var statement = connection.createStatement();
                     var duplicates = statement.executeQuery("SELECT aircraft_id, seat_number FROM seats "
                         + "GROUP BY aircraft_id, seat_number HAVING COUNT(*) > 1")) {
                    if (duplicates.next()) {
                        throw new IllegalStateException("Duplicate aircraft seat numbers exist. "
                            + "Database schema update stopped; inspect records before applying seat uniqueness.");
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Cannot inspect existing seats safely; database schema update stopped", ex);
            }
        };
    }
}
