package com.one211.xds.auth;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Shared utility methods for authentication
 */
public class AuthenticationHelper {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationHelper.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Validate a JWT token and return the username if valid
     *
     * @param token the JWT token
     * @return the username if valid, null otherwise
     */
    public static String validateToken(String token) {
        if (!JwtUtility.validateToken(token)) {
            return null;
        }
        return JwtUtility.extractUsername(token);
    }

    /**
     * Check if an endpoint is public (doesn't require authentication)
     *
     * @param path the endpoint path
     * @return true if the endpoint is public, false otherwise
     */
    public static boolean isPublicEndpoint(String path) {
        return path.equals("/health") ||
               path.equals("/login") ||
               path.equals("/auth/login") ||
               path.startsWith("/docs") ||
               path.startsWith("/swagger");
    }

    /**
     * Send an unauthorized response
     *
     * @param exchange the HTTP exchange
     * @throws IOException if an I/O error occurs
     */
    public static void sendUnauthorizedResponse(HttpExchange exchange) throws IOException {
        String response = "{\"error\":\"Unauthorized\",\"message\":\"Valid authentication token required\"}";
        byte[] responseBytes = response.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(401, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        // Close the response body stream, but NOT the exchange itself
        exchange.getResponseBody().close();
    }

    /**
     * Extract and validate JWT token from request headers
     *
     * @param exchange the HTTP exchange
     * @return the username if valid, null otherwise
     */
    public static String extractAndValidateToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            return validateToken(token);
        }
        return null;
    }

    /**
     * Extract role from authenticated request
     *
     * @param exchange the HTTP exchange
     * @return the role if valid, null otherwise
     */
    public static String extractRole(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            return JwtUtility.extractRole(token);
        }
        return null;
    }

    /**
     * Add user information to request headers for downstream use
     *
     * @param exchange the HTTP exchange
     * @param username the authenticated username
     */
    public static void addUserHeaders(HttpExchange exchange, String username) {
        exchange.getRequestHeaders().add("X-User", username);
        String role = extractRole(exchange);
        if (role != null) {
            exchange.getRequestHeaders().add("X-Role", role);
        }
    }
}
