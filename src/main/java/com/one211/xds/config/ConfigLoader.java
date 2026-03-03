package com.one211.xds.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for loading and accessing application configuration from application.conf
 */
public class ConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private static Config config;

    // Singleton pattern
    private ConfigLoader() {
    }

    /**
     * Load the configuration file
     */
    public static synchronized void loadConfig() {
        if (config == null) {
            config = ConfigFactory.load();
            logger.info("Configuration loaded from application.conf");
        }
    }

    /**
     * Get the configuration instance
     *
     * @return the Config instance
     */
    public static Config getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    /**
     * Get server gRPC port
     *
     * @return gRPC port number
     */
    public static int getGrpcPort() {
        int port = getConfig().getInt("server.grpc.port");
        return getConfigValue("GRPC_PORT", port);
    }

    /**
     * Get server HTTP port
     *
     * @return HTTP port number
     */
    public static int getHttpPort() {
        int port = getConfig().getInt("server.http.port");
        return getConfigValue("HTTP_PORT", port);
    }

    /**
     * Check if authentication is enabled
     *
     * @return true if authentication is enabled, false otherwise
     */
    public static boolean isAuthenticationEnabled() {
        boolean enabled = getConfig().getBoolean("authentication.enabled");
        return !getConfigValue("DISABLE_AUTH", !enabled);
    }

    /**
     * Get JWT secret key
     *
     * @return JWT secret key
     */
    public static String getJwtSecretKey() {
        String envKey = System.getenv("JWT_SECRET_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            logger.info("Using JWT secret key from environment variable");
            return envKey;
        }
        return getConfig().getString("authentication.jwt.secret-key");
    }

    /**
     * Get JWT expiration time in milliseconds
     *
     * @return expiration time in milliseconds
     */
    public static long getJwtExpirationMs() {
        return getConfig().getLong("authentication.jwt.expiration-ms");
    }

    /**
     * Get JWT issuer
     *
     * @return issuer string
     */
    public static String getJwtIssuer() {
        return getConfig().getString("authentication.jwt.issuer");
    }

    /**
     * Get JWT audience
     *
     * @return audience string
     */
    public static String getJwtAudience() {
        return getConfig().getString("authentication.jwt.audience");
    }

    /**
     * Get configured users from config file
     *
     * @return map of username to user credentials
     */
    public static Map<String, UserCredentials> getUsers() {
        Map<String, UserCredentials> users = new HashMap<>();

        if (getConfig().hasPath("authentication.users")) {
            Config usersConfig = getConfig().getConfig("authentication.users");

            for (String username : usersConfig.root().keySet()) {
                Config userConfig = usersConfig.getConfig(username);

                String password = userConfig.getString("password");
                String role = userConfig.getString("role");
                boolean enabled = userConfig.getBoolean("enabled");

                users.put(username, new UserCredentials(username, password, role, enabled));
                logger.info("Loaded user: {} with role: {}", username, role);
            }
        }

        return users;
    }

    /**
     * Get role permissions
     *
     * @return map of role to permissions
     */
    public static Map<String, RolePermissions> getRolePermissions() {
        Map<String, RolePermissions> roles = new HashMap<>();

        if (getConfig().hasPath("authentication.roles")) {
            Config rolesConfig = getConfig().getConfig("authentication.roles");

            for (String roleName : rolesConfig.root().keySet()) {
                Config roleConfig = rolesConfig.getConfig(roleName);

                List<String> permissions = roleConfig.getStringList("permissions");
                String description = roleConfig.getString("description");

                roles.put(roleName, new RolePermissions(roleName, permissions, description));
            }
        }

        return roles;
    }

    /**
     * Get cache node ID
     *
     * @return node ID
     */
    public static String getCacheNodeId() {
        return getConfig().getString("cache.node-id");
    }

    /**
     * Get cache refresh interval in seconds
     *
     * @return refresh interval
     */
    public static int getCacheRefreshIntervalSec() {
        return getConfig().getInt("cache.refresh-interval-sec");
    }

    /**
     * Check if demo mode is enabled
     *
     * @return true if demo mode is enabled
     */
    public static boolean isDemoMode() {
        return getConfig().getBoolean("demo.enabled");
    }

    /**
     * Get configuration value from environment variable or use default
     *
     * @param envVar environment variable name
     * @param defaultValue default value
     * @return value from env or default
     */
    private static int getConfigValue(String envVar, int defaultValue) {
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            try {
                return Integer.parseInt(envValue);
            } catch (NumberFormatException e) {
                logger.warn("Invalid value for {}: {}, using default {}", envVar, envValue, defaultValue);
            }
        }
        return defaultValue;
    }

    private static boolean getConfigValue(String envVar, boolean defaultValue) {
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            return Boolean.parseBoolean(envValue);
        }
        return defaultValue;
    }

    /**
     * Record for user credentials
     */
    public record UserCredentials(String username, String password, String role, boolean enabled) {
    }

    /**
     * Record for role permissions
     */
    public record RolePermissions(String roleName, List<String> permissions, String description) {
    }
}
