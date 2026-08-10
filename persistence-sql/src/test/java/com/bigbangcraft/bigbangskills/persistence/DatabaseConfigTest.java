package com.bigbangcraft.bigbangskills.persistence;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {
    @Test void safeDescriptionNeverContainsPassword() {
        var properties = new Properties();
        properties.setProperty("database.type", "mysql");
        properties.setProperty("database.host", "10.0.0.2");
        properties.setProperty("database.name", "skills");
        properties.setProperty("database.username", "user");
        properties.setProperty("database.password", "secret");
        var config = DatabaseConfig.from(properties);
        assertEquals("MySQL://10.0.0.2:3306/skills", config.safeDescription());
        assertFalse(config.safeDescription().contains("secret"));
        assertFalse(config.jdbcUrl().contains("secret"));
    }
}
