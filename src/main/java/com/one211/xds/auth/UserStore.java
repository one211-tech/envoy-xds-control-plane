package com.one211.xds.auth;

import com.one211.xds.config.ConfigLoader;
import com.one211.xds.auth.JwtUtility;
import org.bouncycastle.crypto.generators.BCrypt;
import org.bouncycastle.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simple in-memory user store for authentication with BCrypt password hashing
 * In production, this should be replaced with a database or LDAP/Active Directory
 */
public class UserStore {

    private static final Logger logger = LoggerFactory.getLogger(UserStore.class);
    private static final int BCRYPT_LOG_ROUNDS = 12;

    private final Map<String, UserCredentials> users = new HashMap<>();

    public UserStore() {
        // Default: load users from config, add fallback admin if needed
        if (users.isEmpty()) {
            loadUsersFromConfig();
        }
    }

    public UserStore(boolean loadFromConfig) {
        if (loadFromConfig) {
            loadUsersFromConfig();
        }
        // If not loading from config, start with empty store
        // Tests can add users explicitly as needed
    }

    /**
     * Load users from application.conf configuration
     */
    private void loadUsersFromConfig() {
        try {
            ConfigLoader.loadConfig();
            Map<String, ConfigLoader.UserCredentials> configUsers = ConfigLoader.getUsers();

            for (ConfigLoader.UserCredentials user : configUsers.values()) {
                if (user.enabled()) {
                    // Hash the password before storing
                    String hashedPassword = hashPassword(user.password());
                    users.put(user.username(), new UserCredentials(hashedPassword, user.role()));
                    logger.info("Loaded user: {} with role: {}", user.username(), user.role());
                } else {
                    logger.warn("User {} is disabled in configuration, skipping", user.username());
                }
            }

            if (users.isEmpty()) {
                logger.warn("No users loaded from config, initializing with default admin user");
                // Fail-fast: DEFAULT_ADMIN_PASSWORD must be set in production
                if (JwtUtility.isProductionEnvironment()) {
                    String errorMsg = "DEFAULT_ADMIN_PASSWORD environment variable must be set in production environments. " +
                                      "Set a secure password for the admin user via environment variable.";
                    logger.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }

                String defaultPassword = System.getenv().getOrDefault("DEFAULT_ADMIN_PASSWORD", "admin123");
                if (JwtUtility.isProductionEnvironment() && defaultPassword.equals("admin123")) {
                    String errorMsg = "DEFAULT_ADMIN_PASSWORD must be changed from the default 'admin123' in production. " +
                                      "Set a secure password via DEFAULT_ADMIN_PASSWORD environment variable.";
                    logger.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }

                addUser("admin", defaultPassword, "ADMIN");
            }

            logger.info("UserStore initialized with {} user(s)", users.size());
        } catch (Exception e) {
            logger.error("Failed to load users from config, using fallback", e);
            String defaultPassword = System.getenv().getOrDefault("DEFAULT_ADMIN_PASSWORD", "admin123");
            addUser("admin", defaultPassword, "ADMIN");
            logger.info("UserStore initialized with fallback admin user");
        }
    }

    /**
     * Hash a password using BCrypt
     *
     * @param password the plain text password
     * @return the hashed password
     */
    private String hashPassword(String password) {
        // Generate a random salt (16 bytes = 128 bits)
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        byte[] hashed = BCrypt.generate(password.getBytes(StandardCharsets.UTF_8), salt, BCRYPT_LOG_ROUNDS);
        return bytesToHex(salt) + ":" + bytesToHex(hashed);
    }

    /**
     * Check if a password matches the stored hash
     *
     * @param password the plain text password
     * @param hashedPassword the stored BCrypt hash
     * @return true if the password matches
     */
    private boolean checkPassword(String password, String hashedPassword) {
        try {
            String[] parts = hashedPassword.split(":");
            if (parts.length != 2) {
                return false;
            }

            byte[] salt = hexToBytes(parts[0]);
            byte[] storedHash = hexToBytes(parts[1]);
            byte[] computedHash = BCrypt.generate(password.getBytes(StandardCharsets.UTF_8), salt, BCRYPT_LOG_ROUNDS);

            // Compare the hashes in constant time
            return Arrays.constantTimeAreEqual(storedHash, computedHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Add a user to the store with BCrypt hashed password
     *
     * @param username the username
     * @param password the password (will be hashed)
     * @param role the user role
     */
    public void addUser(String username, String password, String role) {
        String hashedPassword = hashPassword(password);
        users.put(username, new UserCredentials(hashedPassword, role));
        logger.info("User {} added with role {}", username, role);
    }

    /**
     * Authenticate a user
     *
     * @param username the username
     * @param password the password
     * @return true if authentication is successful, false otherwise
     */
    public boolean authenticate(String username, String password) {
        UserCredentials credentials = users.get(username);
        if (credentials == null) {
            logger.warn("Authentication failed: user {} not found", username);
            return false;
        }

        boolean authenticated = checkPassword(password, credentials.password());
        if (authenticated) {
            logger.info("User {} authenticated successfully", username);
        } else {
            logger.warn("Authentication failed for user {}", username);
        }
        return authenticated;
    }

    /**
     * Get a user's role
     *
     * @param username the username
     * @return Optional containing the role, or empty if user not found
     */
    public Optional<String> getRole(String username) {
        UserCredentials credentials = users.get(username);
        return credentials != null ? Optional.of(credentials.role()) : Optional.empty();
    }

    /**
     * Check if a user exists
     *
     * @param username the username
     * @return true if the user exists, false otherwise
     */
    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    /**
     * Remove a user from the store
     *
     * @param username the username
     * @return true if the user was removed, false if not found
     */
    public boolean removeUser(String username) {
        if (users.containsKey(username)) {
            users.remove(username);
            logger.info("User {} removed from store", username);
            return true;
        }
        return false;
    }

    /**
     * Check if a user has a specific permission
     *
     * @param username the username
     * @param permission the permission to check
     * @return true if the user has the permission, false otherwise
     */
    public boolean hasPermission(String username, String permission) {
        Optional<String> role = getRole(username);
        if (role.isEmpty()) {
            return false;
        }

        try {
            ConfigLoader.loadConfig();
            Map<String, ConfigLoader.RolePermissions> roles = ConfigLoader.getRolePermissions();
            ConfigLoader.RolePermissions rolePerms = roles.get(role.get());
            return rolePerms != null && rolePerms.permissions().contains(permission);
        } catch (Exception e) {
            logger.warn("Failed to check permission {} for user {}", permission, username, e);
            return false;
        }
    }

    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Convert hex string to bytes
     */
    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

    /**
     * Record to hold user credentials
     */
    private record UserCredentials(String password, String role) {
    }
}
