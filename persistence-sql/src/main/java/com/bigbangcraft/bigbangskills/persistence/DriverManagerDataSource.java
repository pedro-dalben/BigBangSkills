package com.bigbangcraft.bigbangskills.persistence;

import java.io.PrintWriter;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class DriverManagerDataSource implements DataSource {
    private final String url;
    private final Properties properties = new Properties();
    public DriverManagerDataSource(String url) { this.url = url; }
    public DriverManagerDataSource property(String key, String value) { properties.setProperty(key, value); return this; }
    @Override public Connection getConnection() throws SQLException { return DriverManager.getConnection(url, properties); }
    @Override public Connection getConnection(String username, String password) throws SQLException { return DriverManager.getConnection(url, username, password); }
    @Override public PrintWriter getLogWriter() { return null; }
    @Override public void setLogWriter(PrintWriter out) {}
    @Override public void setLoginTimeout(int seconds) throws SQLException { DriverManager.setLoginTimeout(seconds); }
    @Override public int getLoginTimeout() { return DriverManager.getLoginTimeout(); }
    @Override public Logger getParentLogger() { return Logger.getLogger("bigbangskills"); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not a wrapper"); }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
}
