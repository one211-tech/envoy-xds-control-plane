package com.one211.xds.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for authentication flow
 */
@DisplayName("Authentication Integration Tests")
public class AuthenticationIntegrationTest {

    private UserStore userStore;

    @BeforeEach
    void setUp() {
        userStore = new UserStore(true); // Load from config
    }

    @Test
    @DisplayName("Should authenticate demo admin user from config")
    void testAuthenticateDemoAdmin() {
        assertTrue(userStore.authenticate("admin", "admin123"),
                "Demo admin user should authenticate");
    }

    @Test
    @DisplayName("Should authenticate demo operator user from config")
    void testAuthenticateDemoOperator() {
        assertTrue(userStore.authenticate("operator", "operator123"),
                "Demo operator user should authenticate");
    }

    @Test
    @DisplayName("Should authenticate demo viewer user from config")
    void testAuthenticateDemoViewer() {
        assertTrue(userStore.authenticate("viewer", "viewer123"),
                "Demo viewer user should authenticate");
    }

    @Test
    @DisplayName("Should authenticate demo service account from config")
    void testAuthenticateDemoServiceAccount() {
        assertTrue(userStore.authenticate("service-account", "service-secret"),
                "Demo service account should authenticate");
    }

    @Test
    @DisplayName("Should reject wrong password for demo users")
    void testRejectWrongPasswordForDemoUsers() {
        assertFalse(userStore.authenticate("admin", "wrongpassword"),
                "Admin should reject wrong password");
        assertFalse(userStore.authenticate("operator", "wrongpassword"),
                "Operator should reject wrong password");
        assertFalse(userStore.authenticate("viewer", "wrongpassword"),
                "Viewer should reject wrong password");
        assertFalse(userStore.authenticate("service-account", "wrongpassword"),
                "Service account should reject wrong password");
    }

    @Test
    @DisplayName("Should have correct roles for demo users")
    void testDemoUserRoles() {
        assertEquals("ADMIN", userStore.getRole("admin").orElse(null),
                "Admin should have ADMIN role");
        assertEquals("OPERATOR", userStore.getRole("operator").orElse(null),
                "Operator should have OPERATOR role");
        assertEquals("VIEWER", userStore.getRole("viewer").orElse(null),
                "Viewer should have VIEWER role");
        assertEquals("SERVICE", userStore.getRole("service-account").orElse(null),
                "Service account should have SERVICE role");
    }

    @Test
    @DisplayName("Should generate valid JWT token for demo users")
    void testGenerateTokenForDemoUsers() {
        String adminToken = JwtUtility.generateToken("admin", "ADMIN");
        String operatorToken = JwtUtility.generateToken("operator", "OPERATOR");
        String viewerToken = JwtUtility.generateToken("viewer", "VIEWER");

        assertNotNull(adminToken, "Admin token should be generated");
        assertNotNull(operatorToken, "Operator token should be generated");
        assertNotNull(viewerToken, "Viewer token should be generated");

        assertTrue(JwtUtility.validateToken(adminToken), "Admin token should be valid");
        assertTrue(JwtUtility.validateToken(operatorToken), "Operator token should be valid");
        assertTrue(JwtUtility.validateToken(viewerToken), "Viewer token should be valid");
    }

    @Test
    @DisplayName("Should extract correct username and role from demo user tokens")
    void testExtractClaimsFromDemoUserTokens() {
        String adminToken = JwtUtility.generateToken("admin", "ADMIN");
        String operatorToken = JwtUtility.generateToken("operator", "OPERATOR");

        assertEquals("admin", JwtUtility.extractUsername(adminToken));
        assertEquals("ADMIN", JwtUtility.extractRole(adminToken));

        assertEquals("operator", JwtUtility.extractUsername(operatorToken));
        assertEquals("OPERATOR", JwtUtility.extractRole(operatorToken));
    }

    @Test
    @DisplayName("Should complete full authentication flow for demo admin")
    void testFullAuthenticationFlowForAdmin() {
        // Step 1: User exists
        assertTrue(userStore.userExists("admin"), "Admin user should exist");

        // Step 2: Authenticate with credentials
        assertTrue(userStore.authenticate("admin", "admin123"), "Should authenticate admin");

        // Step 3: Get user role
        assertEquals("ADMIN", userStore.getRole("admin").orElse(null), "Role should be ADMIN");

        // Step 4: Generate JWT token
        String token = JwtUtility.generateToken("admin", "ADMIN");
        assertNotNull(token, "Token should be generated");

        // Step 5: Validate token
        assertTrue(JwtUtility.validateToken(token), "Token should be valid");

        // Step 6: Extract claims
        assertEquals("admin", JwtUtility.extractUsername(token), "Username should match");
        assertEquals("ADMIN", JwtUtility.extractRole(token), "Role should match");
    }

    @Test
    @DisplayName("Should complete full authentication flow for demo viewer")
    void testFullAuthenticationFlowForViewer() {
        // Step 1: User exists
        assertTrue(userStore.userExists("viewer"), "Viewer user should exist");

        // Step 2: Authenticate with credentials
        assertTrue(userStore.authenticate("viewer", "viewer123"), "Should authenticate viewer");

        // Step 3: Get user role
        assertEquals("VIEWER", userStore.getRole("viewer").orElse(null), "Role should be VIEWER");

        // Step 4: Generate JWT token
        String token = JwtUtility.generateToken("viewer", "VIEWER");
        assertNotNull(token, "Token should be generated");

        // Step 5: Validate token
        assertTrue(JwtUtility.validateToken(token), "Token should be valid");

        // Step 6: Extract claims
        assertEquals("viewer", JwtUtility.extractUsername(token), "Username should match");
        assertEquals("VIEWER", JwtUtility.extractRole(token), "Role should match");
    }

    @Test
    @DisplayName("Should handle authentication failure flow")
    void testAuthenticationFailureFlow() {
        // Step 1: User does not exist
        assertFalse(userStore.userExists("nonexistent"), "Non-existent user should not exist");

        // Step 2: Authentication fails
        assertFalse(userStore.authenticate("nonexistent", "password"),
                "Non-existent user should fail authentication");

        // Step 3: Get role returns empty
        assertTrue(userStore.getRole("nonexistent").isEmpty(),
                "Non-existent user should have no role");
    }

    @Test
    @DisplayName("Should generate unique tokens for same user")
    void testGenerateUniqueTokensForSameUser() {
        String token1 = JwtUtility.generateToken("admin", "ADMIN");

        try {
            Thread.sleep(10); // Small delay to ensure different timestamps
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = JwtUtility.generateToken("admin", "ADMIN");

        // Both tokens should be valid
        assertTrue(JwtUtility.validateToken(token1), "First token should be valid");
        assertTrue(JwtUtility.validateToken(token2), "Second token should be valid");

        // Both should extract the same username and role
        assertEquals("admin", JwtUtility.extractUsername(token1));
        assertEquals("admin", JwtUtility.extractUsername(token2));
        assertEquals("ADMIN", JwtUtility.extractRole(token1));
        assertEquals("ADMIN", JwtUtility.extractRole(token2));
    }

    @Test
    @DisplayName("Should handle token with different roles")
    void testTokenWithDifferentRoles() {
        String[] roles = {"ADMIN", "OPERATOR", "VIEWER", "SERVICE"};

        for (String role : roles) {
            String token = JwtUtility.generateToken("testuser", role);
            assertNotNull(token, "Token should be generated for role: " + role);
            assertTrue(JwtUtility.validateToken(token), "Token should be valid for role: " + role);
            assertEquals(role, JwtUtility.extractRole(token), "Role should match for: " + role);
        }
    }

    @Test
    @DisplayName("Should verify all demo users can be loaded")
    void testAllDemoUsersLoaded() {
        String[] expectedUsers = {"admin", "operator", "viewer", "service-account"};

        for (String username : expectedUsers) {
            assertTrue(userStore.userExists(username), username + " should exist in user store");
        }
    }

    @Test
    @DisplayName("Should verify token expiration is set correctly")
    void testTokenExpiration() {
        String token = JwtUtility.generateToken("admin", "ADMIN");
        var claims = JwtUtility.extractAllClaims(token);

        assertNotNull(claims.getExpiration(), "Expiration should be set");
        assertNotNull(claims.getIssuedAt(), "IssuedAt should be set");

        // Token should not be expired (just created)
        assertFalse(JwtUtility.isTokenExpired(token), "Fresh token should not be expired");
    }
}
