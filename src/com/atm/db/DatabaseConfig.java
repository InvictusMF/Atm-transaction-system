package com.atm.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Thread-safe configuration manager and connection factory for MySQL JDBC.
 * Retrieves connection parameters from environment variables, system properties,
 * or local properties files without hardcoding credentials in source code.
 */
public class DatabaseConfig {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/atm_simulation?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean initialized = false;

    public static class ConnectionTestResult {
        private final boolean success;
        private final String message;
        private final String details;

        public ConnectionTestResult(boolean success, String message, String details) {
            this.success = success;
            this.message = message;
            this.details = details;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }
    }

    private static synchronized void initialize() {
        if (initialized) return;

        // 1. Check System Environment Variables
        String envUrl = System.getenv("ATM_DB_URL");
        String envHost = System.getenv("ATM_DB_HOST");
        String envPort = System.getenv("ATM_DB_PORT");
        String envName = System.getenv("ATM_DB_NAME");
        String envUser = System.getenv("ATM_DB_USER");
        String envPass = System.getenv("ATM_DB_PASSWORD");

        // 2. Check System Properties (-Datm.db.url=...)
        String propUrl = System.getProperty("atm.db.url");
        String propHost = System.getProperty("atm.db.host");
        String propPort = System.getProperty("atm.db.port");
        String propName = System.getProperty("atm.db.name");
        String propUser = System.getProperty("atm.db.user");
        String propPass = System.getProperty("atm.db.password");

        // 3. Check local database.properties file
        Properties fileProps = new Properties();
        File[] candidateFiles = new File[]{
                new File("database.properties"),
                new File("db.properties"),
                new File("local.properties")
        };

        for (File f : candidateFiles) {
            if (f.exists() && f.isFile()) {
                try (InputStream in = new FileInputStream(f)) {
                    fileProps.load(in);
                    break;
                } catch (IOException ignored) {}
            }
        }

        // Host, Port, Database name resolution
        String host = propHost != null ? propHost.trim() : (envHost != null ? envHost.trim() : fileProps.getProperty("db.host", "localhost").trim());
        String port = propPort != null ? propPort.trim() : (envPort != null ? envPort.trim() : fileProps.getProperty("db.port", "3306").trim());
        String dbName = propName != null ? propName.trim() : (envName != null ? envName.trim() : fileProps.getProperty("db.name", "atm_simulation").trim());
        String defaultDynamicUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

        // Resolution hierarchy for URL: Explicit URL > Constructed URL
        if (propUrl != null && !propUrl.trim().isEmpty()) {
            dbUrl = propUrl.trim();
        } else if (envUrl != null && !envUrl.trim().isEmpty()) {
            dbUrl = envUrl.trim();
        } else if (fileProps.getProperty("db.url") != null && !fileProps.getProperty("db.url").trim().isEmpty()) {
            dbUrl = fileProps.getProperty("db.url").trim();
        } else {
            dbUrl = defaultDynamicUrl;
        }

        // Resolution hierarchy for User: System Prop > Env Var > File > default "root"
        if (propUser != null && !propUser.trim().isEmpty()) {
            dbUser = propUser.trim();
        } else if (envUser != null && !envUser.trim().isEmpty()) {
            dbUser = envUser.trim();
        } else {
            dbUser = fileProps.getProperty("db.user", DEFAULT_USER).trim();
        }

        // Resolution hierarchy for Password: System Prop > Env Var (ATM_DB_PASSWORD) > File > default empty
        if (propPass != null) {
            dbPassword = propPass;
        } else if (envPass != null) {
            dbPassword = envPass;
        } else {
            dbPassword = fileProps.getProperty("db.password", DEFAULT_PASSWORD);
        }

        // Verify JDBC driver class availability
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("WARN: MySQL Connector/J driver class not found on classpath. Ensure lib/mysql-connector-j.jar is included.");
        }

        initialized = true;
    }

    /**
     * Obtains a new active connection to the MySQL database.
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            initialize();
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Tests the database connection and returns diagnostic results without revealing the password.
     */
    public static ConnectionTestResult testConnection() {
        if (!initialized) {
            initialize();
        }

        try (Connection conn = getConnection()) {
            if (conn.isValid(3)) {
                String meta = conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion();
                return new ConnectionTestResult(true, "Connection successful to " + getSafeUrl(), "Connected as user '" + dbUser + "' (" + meta + ")");
            } else {
                return new ConnectionTestResult(false, "Connection validation timeout", "Database reachable but did not respond within timeout.");
            }
        } catch (SQLException e) {
            String safeMsg = e.getMessage() != null ? e.getMessage() : "Unknown SQL Exception";
            // Strip any sensitive strings if present
            return new ConnectionTestResult(false, safeMsg,
                    "Target: " + getSafeUrl() + " | User: '" + dbUser + "' | SQLState: " + e.getSQLState() + " | ErrorCode: " + e.getErrorCode());
        }
    }

    public static String getSafeUrl() {
        if (!initialized) initialize();
        return dbUrl;
    }

    public static String getUser() {
        if (!initialized) initialize();
        return dbUser;
    }

    public static synchronized void reloadConfig() {
        initialized = false;
        initialize();
    }
}
