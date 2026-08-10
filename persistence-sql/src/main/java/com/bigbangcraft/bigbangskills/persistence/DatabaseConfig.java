package com.bigbangcraft.bigbangskills.persistence;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javax.sql.DataSource;

public record DatabaseConfig(Type type, String sqliteFile, String host, int port, String database, String username, String password, int maxPoolSize, int minIdle, long connectionTimeoutMillis) {
    public enum Type { SQLITE, MYSQL, MARIADB }

    public DatabaseConfig {
        if (type == null || sqliteFile == null || sqliteFile.isBlank() || host == null || host.isBlank() || port < 1 || port > 65535 || database == null || database.isBlank() || username == null || maxPoolSize < 1 || minIdle < 0 || minIdle > maxPoolSize || connectionTimeoutMillis < 250) throw new IllegalArgumentException("Invalid database configuration");
        password = password == null ? "" : password;
    }

    public static DatabaseConfig loadOrCreate(Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().normalize().getParent());
        if (Files.notExists(file)) Files.writeString(file, defaultsText());
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) { properties.load(reader); }
        return from(properties);
    }

    public static DatabaseConfig from(Properties properties) {
        var type = Type.valueOf(properties.getProperty("database.type", "sqlite").trim().toUpperCase());
        return new DatabaseConfig(type,
            properties.getProperty("database.sqlite.file", "bigbangskills.db").trim(),
            properties.getProperty("database.host", "127.0.0.1").trim(),
            integer(properties, "database.port", 3306),
            properties.getProperty("database.name", "bigbangskills").trim(),
            properties.getProperty("database.username", "bigbangskills").trim(),
            properties.getProperty("database.password", ""),
            integer(properties, "database.pool.max_size", 8),
            integer(properties, "database.pool.min_idle", 1),
            longValue(properties, "database.pool.connection_timeout_ms", 3000));
    }

    public DataSource createDataSource() {
        var dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl());
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(maxPoolSize);
        dataSource.setMinimumIdle(minIdle);
        dataSource.setConnectionTimeout(connectionTimeoutMillis);
        dataSource.setPoolName("bigbangskills");
        dataSource.setInitializationFailTimeout(-1);
        if (type == Type.SQLITE) dataSource.setConnectionTestQuery("SELECT 1");
        return dataSource;
    }

    public String jdbcUrl() {
        return switch (type) {
            case SQLITE -> "jdbc:sqlite:" + sqliteFile;
            case MYSQL -> "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC";
            case MARIADB -> "jdbc:mariadb://" + host + ":" + port + "/" + database + "?useSsl=false";
        };
    }

    public String safeDescription() {
        return switch (type) {
            case SQLITE -> "SQLite:" + sqliteFile;
            case MYSQL -> "MySQL://" + host + ":" + port + "/" + database;
            case MARIADB -> "MariaDB://" + host + ":" + port + "/" + database;
        };
    }

    private static int integer(Properties properties, String key, int fallback) {
        try { return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim()); }
        catch (NumberFormatException failure) { throw new IllegalArgumentException("Invalid integer: " + key, failure); }
    }

    private static long longValue(Properties properties, String key, long fallback) {
        try { return Long.parseLong(properties.getProperty(key, Long.toString(fallback)).trim()); }
        catch (NumberFormatException failure) { throw new IllegalArgumentException("Invalid number: " + key, failure); }
    }

    private static String defaultsText() {
        return "# BigBangSkills JDBC configuration; password is never logged.\n" +
            "database.type=sqlite\n" +
            "database.sqlite.file=bigbangskills.db\n" +
            "database.host=127.0.0.1\n" +
            "database.port=3306\n" +
            "database.name=bigbangskills\n" +
            "database.username=bigbangskills\n" +
            "database.password=\n" +
            "database.pool.max_size=8\n" +
            "database.pool.min_idle=1\n" +
            "database.pool.connection_timeout_ms=3000\n";
    }
}
