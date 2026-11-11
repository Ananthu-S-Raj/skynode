package com.skynode;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.core.env.Environment;
import javax.sql.DataSource;

@SpringBootTest
@ActiveProfiles("test")
class SkyNodeApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads(ConfigurableApplicationContext context) {
        Environment env = context.getEnvironment();
        String databaseName = env.getProperty("spring.datasource.url");
        
        // Safety check: verify we are connecting to the test database
        if (databaseName == null || !databaseName.contains("skynode_test")) {
            throw new IllegalStateException(
                "Test must connect to skynode_test database. " +
                "Current datasource URL: " + databaseName
            );
        }
    }

    @Test
    void verifyTestDatabaseConnection() throws Exception {
        try (java.sql.Connection conn = dataSource.getConnection()) {
            String catalog = conn.getCatalog();
            if (catalog == null || !catalog.equals("skynode_test")) {
                throw new IllegalStateException(
                    "Expected database name 'skynode_test', but got: " + catalog
                );
            }
        }
    }
}