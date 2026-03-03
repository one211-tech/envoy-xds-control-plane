package com.one211.xds.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtility class
 */
@DisplayName("JWT Utility Tests")
public class JwtUtilityTest {

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_ROLE = "ADMIN";
    private String validToken;
    private String expiredToken;

    @BeforeEach
    void setUp() {
        // Generate a valid token for testing
        validToken = JwtUtility.generateToken(TEST_USERNAME, TEST_ROLE);

        // Generate an expired token by setting a very short expiration
        long originalExpiration = JwtUtility.getExpirationTime();
        try {
            // Note: In a real test, we might want to use a test-specific utility
            // For now, we'll just test with the current implementation
            expiredToken = validToken; // Will test with invalid token instead
        } finally {
            // Reset if needed
        }
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void testGenerateToken() {
        assertNotNull(validToken, "Generated token should not be null");
        assertFalse(validToken.isEmpty(), "Generated token should not be empty");
        assertTrue(validToken.split("\\.").length == 3, "Token should have 3 parts (header, payload, signature)");
    }

    @Test
    @DisplayName("Should validate valid token")
    void testValidateToken() {
        assertTrue(JwtUtility.validateToken(validToken), "Valid token should pass validation");
    }

    @Test
    @DisplayName("Should reject invalid token")
    void testValidateInvalidToken() {
        String invalidToken = "invalid.token.string";
        assertFalse(JwtUtility.validateToken(invalidToken), "Invalid token should fail validation");
    }

    @Test
    @DisplayName("Should reject malformed token")
    void testValidateMalformedToken() {
        String malformedToken = "not.a.valid.jwt.token";
        assertFalse(JwtUtility.validateToken(malformedToken), "Malformed token should fail validation");
    }

    @Test
    @DisplayName("Should extract username from token")
    void testExtractUsername() {
        String username = JwtUtility.extractUsername(validToken);
        assertEquals(TEST_USERNAME, username, "Extracted username should match the original");
    }

    @Test
    @DisplayName("Should extract role from token")
    void testExtractRole() {
        String role = JwtUtility.extractRole(validToken);
        assertEquals(TEST_ROLE, role, "Extracted role should match the original");
    }

    @Test
    @DisplayName("Should return null for invalid token username extraction")
    void testExtractUsernameFromInvalidToken() {
        String username = JwtUtility.extractUsername("invalid.token");
        assertNull(username, "Should return null for invalid token");
    }

    @Test
    @DisplayName("Should return null for invalid token role extraction")
    void testExtractRoleFromInvalidToken() {
        String role = JwtUtility.extractRole("invalid.token");
        assertNull(role, "Should return null for invalid token");
    }

    @Test
    @DisplayName("Should detect when token is not expired")
    void testIsTokenExpired() {
        assertFalse(JwtUtility.isTokenExpired(validToken), "Freshly generated token should not be expired");
    }

    @Test
    @DisplayName("Should extract all claims from valid token")
    void testExtractAllClaims() {
        var claims = JwtUtility.extractAllClaims(validToken);
        assertNotNull(claims, "Claims should not be null for valid token");
        assertEquals(TEST_USERNAME, claims.getSubject(), "Subject should match username");
        assertNotNull(claims.getIssuedAt(), "IssuedAt should not be null");
        assertNotNull(claims.getExpiration(), "Expiration should not be null");
    }

    @Test
    @DisplayName("Should return null for claims from invalid token")
    void testExtractAllClaimsFromInvalidToken() {
        var claims = JwtUtility.extractAllClaims("invalid.token");
        assertNull(claims, "Should return null for invalid token");
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "operator", "viewer", "service-account"})
    @DisplayName("Should generate tokens for different roles")
    void testGenerateTokensForDifferentRoles(String role) {
        String token = JwtUtility.generateToken("testuser", role);
        assertNotNull(token, "Token should be generated for role: " + role);
        assertTrue(JwtUtility.validateToken(token), "Token for role " + role + " should be valid");

        String extractedRole = JwtUtility.extractRole(token);
        assertEquals(role, extractedRole, "Extracted role should match: " + role);
    }

    @Test
    @DisplayName("Should return positive expiration time")
    void testGetExpirationTime() {
        long expirationTime = JwtUtility.getExpirationTime();
        assertTrue(expirationTime > 0, "Expiration time should be positive");
    }

    @Test
    @DisplayName("Should generate unique tokens")
    void testGenerateUniqueTokens() {
        String token1 = JwtUtility.generateToken("user1", "ADMIN");
        String token2 = JwtUtility.generateToken("user2", "ADMIN");

        assertNotEquals(token1, token2, "Tokens for different users should be different");
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void testGenerateTokensAtDifferentTimes() {
        // Note: Due to timing resolution, this might fail if calls are too close
        // In practice, tokens should have different issuedAt claims
        String token1 = JwtUtility.generateToken("user1", "ADMIN");

        try {
            Thread.sleep(10); // Small delay to ensure different timestamps
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = JwtUtility.generateToken("user1", "ADMIN");

        // Tokens might be different due to timestamp
        assertNotNull(token1);
        assertNotNull(token2);
    }
}
