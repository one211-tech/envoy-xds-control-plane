package com.one211.xds.auth;

import com.one211.xds.config.ConfigLoader;

import java.util.Map;

/**
 * Demo class showcasing the authentication system
 * Run this class to see authentication in action
 */
public class AuthenticationDemo {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("xDS Control Plane - Authentication Demo");
        System.out.println("=".repeat(60));
        System.out.println();

        // Demo 1: Load Configuration
        System.out.println("Demo 1: Loading Configuration");
        System.out.println("-".repeat(60));
        ConfigLoader.loadConfig();
        System.out.println("gRPC Port: " + ConfigLoader.getGrpcPort());
        System.out.println("HTTP Port: " + ConfigLoader.getHttpPort());
        System.out.println("Authentication Enabled: " + ConfigLoader.isAuthenticationEnabled());
        System.out.println("JWT Expiration: " + ConfigLoader.getJwtExpirationMs() + "ms");
        System.out.println();

        // Demo 2: Load Users from Config
        System.out.println("Demo 2: Loading Users from Configuration");
        System.out.println("-".repeat(60));
        UserStore userStore = new UserStore(true);
        Map<String, ConfigLoader.UserCredentials> users = ConfigLoader.getUsers();
        for (ConfigLoader.UserCredentials user : users.values()) {
            System.out.printf("User: %-15s Role: %-10s Enabled: %s%n",
                    user.username(), user.role(), user.enabled());
        }
        System.out.println();

        // Demo 3: Authenticate Demo Users
        System.out.println("Demo 3: Authenticating Demo Users");
        System.out.println("-".repeat(60));
        authenticateDemoUser(userStore, "admin", "admin123");
        authenticateDemoUser(userStore, "operator", "operator123");
        authenticateDemoUser(userStore, "viewer", "viewer123");
        authenticateDemoUser(userStore, "service-account", "service-secret");
        System.out.println();

        // Demo 4: Generate JWT Tokens
        System.out.println("Demo 4: Generating JWT Tokens");
        System.out.println("-".repeat(60));
        String adminToken = JwtUtility.generateToken("admin", "ADMIN");
        String viewerToken = JwtUtility.generateToken("viewer", "VIEWER");
        System.out.println("Admin Token: " + adminToken.substring(0, 50) + "...");
        System.out.println("Viewer Token: " + viewerToken.substring(0, 50) + "...");
        System.out.println();

        // Demo 5: Validate Tokens
        System.out.println("Demo 5: Validating JWT Tokens");
        System.out.println("-".repeat(60));
        validateAndExtractToken(adminToken);
        validateAndExtractToken(viewerToken);
        System.out.println();

        // Demo 6: Role Permissions
        System.out.println("Demo 6: Role Permissions");
        System.out.println("-".repeat(60));
        Map<String, ConfigLoader.RolePermissions> roles = ConfigLoader.getRolePermissions();
        for (ConfigLoader.RolePermissions role : roles.values()) {
            System.out.printf("Role: %-10s Permissions: %s%n",
                    role.roleName(), String.join(", ", role.permissions()));
        }
        System.out.println();

        // Demo 7: Authentication Flow
        System.out.println("Demo 7: Complete Authentication Flow");
        System.out.println("-".repeat(60));
        demonstrateCompleteFlow(userStore, "admin", "admin123");
        System.out.println();

        // Demo 8: Error Cases
        System.out.println("Demo 8: Error Cases");
        System.out.println("-".repeat(60));
        demonstrateErrorCases(userStore);
        System.out.println();

        System.out.println("=".repeat(60));
        System.out.println("Authentication Demo Complete!");
        System.out.println("=".repeat(60));
    }

    private static void authenticateDemoUser(UserStore userStore, String username, String password) {
        boolean authenticated = userStore.authenticate(username, password);
        if (authenticated) {
            String role = userStore.getRole(username).orElse("UNKNOWN");
            System.out.printf("✓ %-15s authenticated successfully (Role: %s)%n", username, role);
        } else {
            System.out.printf("✗ %-15s authentication failed%n", username);
        }
    }

    private static void validateAndExtractToken(String token) {
        boolean valid = JwtUtility.validateToken(token);
        if (valid) {
            String username = JwtUtility.extractUsername(token);
            String role = JwtUtility.extractRole(token);
            var claims = JwtUtility.extractAllClaims(token);
            System.out.printf("✓ Token valid - User: %-10s Role: %-10s Expires: %s%n",
                    username, role, claims.getExpiration());
        } else {
            System.out.println("✗ Token invalid");
        }
    }

    private static void demonstrateCompleteFlow(UserStore userStore, String username, String password) {
        System.out.println("Step 1: Check if user exists");
        boolean exists = userStore.userExists(username);
        System.out.println("  User '" + username + "' exists: " + exists);

        System.out.println("\nStep 2: Authenticate with credentials");
        boolean authenticated = userStore.authenticate(username, password);
        System.out.println("  Authentication: " + (authenticated ? "SUCCESS" : "FAILED"));

        if (authenticated) {
            System.out.println("\nStep 3: Get user role");
            String role = userStore.getRole(username).orElse("UNKNOWN");
            System.out.println("  Role: " + role);

            System.out.println("\nStep 4: Generate JWT token");
            String token = JwtUtility.generateToken(username, role);
            System.out.println("  Token: " + token.substring(0, 30) + "...");

            System.out.println("\nStep 5: Validate token");
            boolean valid = JwtUtility.validateToken(token);
            System.out.println("  Token valid: " + valid);

            System.out.println("\nStep 6: Extract claims");
            System.out.println("  Username: " + JwtUtility.extractUsername(token));
            System.out.println("  Role: " + JwtUtility.extractRole(token));
            System.out.println("  Expired: " + JwtUtility.isTokenExpired(token));

            System.out.println("\n✓ Complete authentication flow successful!");
        }
    }

    private static void demonstrateErrorCases(UserStore userStore) {
        System.out.println("Case 1: Wrong password");
        boolean result = userStore.authenticate("admin", "wrongpassword");
        System.out.println("  Result: " + (result ? "Unexpectedly authenticated" : "Correctly rejected"));

        System.out.println("\nCase 2: Non-existent user");
        result = userStore.authenticate("nonexistent", "password");
        System.out.println("  Result: " + (result ? "Unexpectedly authenticated" : "Correctly rejected"));

        System.out.println("\nCase 3: Invalid token");
        boolean valid = JwtUtility.validateToken("invalid.token.string");
        System.out.println("  Result: " + (valid ? "Unexpectedly valid" : "Correctly rejected"));

        System.out.println("\nCase 4: Malformed token");
        valid = JwtUtility.validateToken("not.a.valid.jwt.token.at.all");
        System.out.println("  Result: " + (valid ? "Unexpectedly valid" : "Correctly rejected"));

        System.out.println("\nCase 5: Empty token");
        valid = JwtUtility.validateToken("");
        System.out.println("  Result: " + (valid ? "Unexpectedly valid" : "Correctly rejected"));
    }
}
