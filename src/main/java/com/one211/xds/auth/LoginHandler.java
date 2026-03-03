package com.one211.xds.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * HTTP handler for login endpoint
 */
public class LoginHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final UserStore userStore;

    public LoginHandler(UserStore userStore) {
        this.userStore = userStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            String errorResponse = "{\"error\":\"Method not allowed\"}";
            byte[] errorBytes = errorResponse.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(405, errorBytes.length);
            exchange.getResponseBody().write(errorBytes);
            exchange.close();
            return;
        }

        try {
            // Read request body
            String requestBody = new BufferedReader(new InputStreamReader(
                    exchange.getRequestBody(), StandardCharsets.UTF_8))
                    .lines()
                    .reduce("", (a, b) -> a + b);

            LoginRequest loginRequest = objectMapper.readValue(requestBody, LoginRequest.class);

            logger.info("Login attempt for user: {}", loginRequest.username());

            // Authenticate user
            if (userStore.authenticate(loginRequest.username(), loginRequest.password())) {
                String role = userStore.getRole(loginRequest.username()).orElse("USER");
                String token = JwtUtility.generateToken(loginRequest.username(), role);

                LoginResponse response = new LoginResponse(token, loginRequest.username(), role, JwtUtility.getExpirationTime());
                String jsonResponse = objectMapper.writeValueAsString(response);
                byte[] responseBytes = jsonResponse.getBytes();

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                exchange.getResponseBody().write(responseBytes);
                exchange.close();
                logger.info("Login successful for user: {}", loginRequest.username());
            } else {
                sendErrorResponse(exchange, 401, "Invalid username or password");
                logger.warn("Login failed for user: {}", loginRequest.username());
            }
        } catch (Exception e) {
            logger.error("Error during login", e);
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        ErrorResponse response = new ErrorResponse(String.valueOf(statusCode), message);
        String jsonResponse = objectMapper.writeValueAsString(response);
        byte[] responseBytes = jsonResponse.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private record LoginRequest(String username, String password) {
    }

    private record LoginResponse(String token, String username, String role, long expiresIn) {
    }

    private record ErrorResponse(String status, String message) {
    }
}
