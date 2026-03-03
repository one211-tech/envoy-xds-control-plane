package com.one211.xds.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserStore class
 */
@DisplayName("User Store Tests")
public class UserStoreTest {

    private UserStore userStore;

    @BeforeEach
    void setUp() {
        // Create a fresh UserStore for each test
        userStore = new UserStore(false); // Don't load from config for unit tests
    }

    @Test
    @DisplayName("Should add user successfully")
    void testAddUser() {
        userStore.addUser("testuser", "password123", "USER");

        assertTrue(userStore.userExists("testuser"), "User should exist after adding");
        assertEquals("USER", userStore.getRole("testuser").orElse(null), "Role should match");
    }

    @Test
    @DisplayName("Should authenticate valid user")
    void testAuthenticateValidUser() {
        userStore.addUser("testuser", "password123", "USER");

        assertTrue(userStore.authenticate("testuser", "password123"), "Valid credentials should authenticate");
    }

    @Test
    @DisplayName("Should reject invalid password")
    void testAuthenticateInvalidPassword() {
        userStore.addUser("testuser", "password123", "USER");

        assertFalse(userStore.authenticate("testuser", "wrongpassword"), "Invalid password should be rejected");
    }

    @Test
    @DisplayName("Should reject non-existent user")
    void testAuthenticateNonExistentUser() {
        assertFalse(userStore.authenticate("nonexistent", "password"), "Non-existent user should be rejected");
    }

    @Test
    @DisplayName("Should return empty role for non-existent user")
    void testGetRoleForNonExistentUser() {
        assertTrue(userStore.getRole("nonexistent").isEmpty(), "Role should be empty for non-existent user");
    }

    @Test
    @DisplayName("Should return correct role for existing user")
    void testGetRoleForExistingUser() {
        userStore.addUser("admin", "adminpass", "ADMIN");

        assertEquals("ADMIN", userStore.getRole("admin").orElse(null), "Role should be ADMIN");
    }

    @Test
    @DisplayName("Should check user existence correctly")
    void testUserExists() {
        userStore.addUser("existinguser", "password", "USER");

        assertTrue(userStore.userExists("existinguser"), "User should exist");
        assertFalse(userStore.userExists("nonexistent"), "Non-existent user should not exist");
    }

    @Test
    @DisplayName("Should remove user successfully")
    void testRemoveUser() {
        userStore.addUser("todelete", "password", "USER");

        assertTrue(userStore.removeUser("todelete"), "Should successfully remove existing user");
        assertFalse(userStore.userExists("todelete"), "User should not exist after removal");
    }

    @Test
    @DisplayName("Should return false when removing non-existent user")
    void testRemoveNonExistentUser() {
        assertFalse(userStore.removeUser("nonexistent"), "Should return false for non-existent user");
    }

    @Test
    @DisplayName("Should handle multiple users")
    void testMultipleUsers() {
        userStore.addUser("admin", "adminpass", "ADMIN");
        userStore.addUser("operator", "operatorpass", "OPERATOR");
        userStore.addUser("viewer", "viewerpass", "VIEWER");

        assertTrue(userStore.userExists("admin"), "Admin user should exist");
        assertTrue(userStore.userExists("operator"), "Operator user should exist");
        assertTrue(userStore.userExists("viewer"), "Viewer user should exist");

        assertEquals("ADMIN", userStore.getRole("admin").orElse(null));
        assertEquals("OPERATOR", userStore.getRole("operator").orElse(null));
        assertEquals("VIEWER", userStore.getRole("viewer").orElse(null));
    }

    @Test
    @DisplayName("Should authenticate each user independently")
    void testMultipleUserAuthentication() {
        userStore.addUser("admin", "adminpass", "ADMIN");
        userStore.addUser("operator", "operatorpass", "OPERATOR");

        assertTrue(userStore.authenticate("admin", "adminpass"), "Admin should authenticate");
        assertTrue(userStore.authenticate("operator", "operatorpass"), "Operator should authenticate");
        assertFalse(userStore.authenticate("admin", "operatorpass"), "Admin should not authenticate with operator password");
    }

    @Test
    @DisplayName("Should allow updating user role by replacing user")
    void testUpdateUserRole() {
        userStore.addUser("user1", "pass", "USER");

        assertEquals("USER", userStore.getRole("user1").orElse(null));

        // Update role by adding the user again with new role
        userStore.addUser("user1", "pass", "ADMIN");

        assertEquals("ADMIN", userStore.getRole("user1").orElse(null), "Role should be updated");
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void testSpecialCharactersInUsername() {
        String specialUser = "test.user@example.com";
        userStore.addUser(specialUser, "password", "USER");

        assertTrue(userStore.userExists(specialUser), "User with special characters should exist");
        assertTrue(userStore.authenticate(specialUser, "password"), "Should authenticate special username");
    }

    @Test
    @DisplayName("Should handle empty password")
    void testEmptyPassword() {
        userStore.addUser("nopassuser", "", "USER");

        assertTrue(userStore.authenticate("nopassuser", ""), "Should authenticate with empty password");
        assertFalse(userStore.authenticate("nopassuser", "password"), "Should reject non-empty password");
    }

    @Test
    @DisplayName("Should be case-sensitive for username")
    void testCaseSensitiveUsername() {
        userStore.addUser("Admin", "password", "ADMIN");

        assertTrue(userStore.userExists("Admin"), "Exact case should exist");
        assertFalse(userStore.userExists("admin"), "Different case should not exist");
    }

    @Test
    @DisplayName("Should be case-sensitive for password")
    void testCaseSensitivePassword() {
        userStore.addUser("testuser", "Password123", "USER");

        assertFalse(userStore.authenticate("testuser", "password123"), "Lowercase password should fail");
        assertFalse(userStore.authenticate("testuser", "PASSWORD123"), "Uppercase password should fail");
        assertTrue(userStore.authenticate("testuser", "Password123"), "Exact password should work");
    }
}
