package com.one211.xds.auth;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Helper class for role-based authorization checks
 */
public class AuthorizationHelper {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationHelper.class);

    /**
     * Check if a user has permission to access an endpoint
     *
     * @param exchange the HTTP exchange (must contain X-User and X-Role headers)
     * @param requiredPermission the permission required
     * @param userStore the UserStore for checking permissions
     * @return true if user has permission, false otherwise
     */
    public static boolean checkPermission(HttpExchange exchange, String requiredPermission, UserStore userStore) {
        String username = exchange.getRequestHeaders().getFirst("X-User");
        if (username == null) {
            logger.warn("Authorization check failed: No X-User header found");
            return false;
        }

        boolean hasPermission = userStore.hasPermission(username, requiredPermission);
        if (!hasPermission) {
            logger.warn("User {} does not have permission: {}", username, requiredPermission);
        }
        return hasPermission;
    }

    /**
     * Send a forbidden response when user lacks permissions
     *
     * @param exchange the HTTP exchange
     * @throws IOException if an I/O error occurs
     */
    public static void sendForbiddenResponse(HttpExchange exchange) throws IOException {
        String response = "{\"error\":\"Forbidden\",\"message\":\"You do not have permission to access this resource\"}";
        byte[] responseBytes = response.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(403, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        // Close the response body stream, but NOT the exchange itself
        exchange.getResponseBody().close();
    }

    /**
     * Get required permission for an endpoint path
     *
     * @param path the endpoint path
     * @return the required permission, or null if no specific permission is required
     */
    public static String getRequiredPermission(String path) {
        if (path.equals("/reload")) {
            return "reload";
        } else if (path.equals("/api/clusters")) {
            return "add-cluster";
        } else if (path.equals("/api/endpoints")) {
            return "update-endpoints";
        }
        return null;
    }
}
