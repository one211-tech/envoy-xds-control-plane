package com.one211.xds;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for xDS Control Plane
 *
 * These tests automatically start and stop the xDS Control Plane application.
 */
@TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class XdsControlPlaneApplicationTest {

    private static final String XDS_HOST = System.getProperty("xds.host", "localhost");
    private static final int XDS_HTTP_PORT = Integer.parseInt(System.getProperty("xds.http.port", "18001"));
    private static final String BASE_URL = String.format("http://%s:%d", XDS_HOST, XDS_HTTP_PORT);
    private static XdsControlPlaneApplication application;
    private static Thread applicationThread;

    @BeforeAll
    static void setUp() {
        System.out.println("Starting xDS Control Plane at: " + BASE_URL);

        // Start the application in a separate thread
        application = new XdsControlPlaneApplication();
        applicationThread = new Thread(() -> {
            try {
                application.start();
            } catch (Exception e) {
                System.err.println("Error starting application: " + e.getMessage());
                e.printStackTrace();
            }
        });
        applicationThread.setDaemon(true);
        applicationThread.start();

        // Wait for the application to start
        waitForApplicationToStart();

        System.out.println("xDS Control Plane started successfully");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("Stopping xDS Control Plane...");
        if (application != null) {
            application.stop();
        }
        if (applicationThread != null) {
            try {
                applicationThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("xDS Control Plane stopped");
    }

    private static void waitForApplicationToStart() {
        int maxAttempts = 60; // 30 seconds max
        int attempts = 0;

        while (attempts < maxAttempts) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "/health").openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    connection.disconnect();
                    return;
                }
                connection.disconnect();
            } catch (IOException e) {
                // Connection refused - server not ready yet
            }
            attempts++;
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for application to start", ie);
            }
        }

        throw new RuntimeException("xDS Control Plane did not start within " + (maxAttempts * 500) + "ms");
    }

    @Test
    @Order(1)
    void testHealthCheck() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "/health").openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        String responseBody = readResponse(connection);

        assertEquals(200, responseCode, "Health check should return 200");
        assertTrue(responseBody.contains("\"status\":\"ok\""),
                "Health check response should contain status ok");

        System.out.println("✓ Health check passed");
    }

    @Test
    @Order(2)
    void testLoginEndpoint() throws IOException {
        String loginJson = """
            {
                "username": "admin",
                "password": "admin123"
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "/login").openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(loginJson.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        String responseBody = readResponse(connection);

        assertEquals(200, responseCode, "Login should return 200");
        assertTrue(responseBody.contains("\"token\""), "Login response should contain token");

        System.out.println("✓ Login endpoint test passed");
    }

    @Test
    @Order(3)
    void testPublicEndpointDoesNotRequireAuth() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "/health").openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();

        assertEquals(200, responseCode, "Public endpoint should be accessible without auth");

        System.out.println("✓ Public endpoint test passed");
    }

    @Test
    @Order(4)
    void testProtectedEndpointRequiresAuth() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "/api/clusters").openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setDoOutput(true);

        int responseCode = connection.getResponseCode();

        assertEquals(401, responseCode, "Protected endpoint should return 401 without auth");

        System.out.println("✓ Protected endpoint auth required test passed");
    }

    @Test
    @Order(6)
    void testConcurrentRequests() throws IOException, InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        final int[] successCount = new int[1];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "/health").openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    if (connection.getResponseCode() == 200) {
                        successCount[0]++;
                    }
                } catch (IOException e) {
                    // Ignore
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertTrue(successCount[0] >= threadCount - 2,
                "Most concurrent requests should succeed");

        System.out.println("✓ Concurrent requests test passed (" + successCount[0] + "/" + threadCount + " succeeded)");
    }

    @Test
    @Order(7)
    void testHealthCheckReturnsJson() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "/health").openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        String responseBody = readResponse(connection);

        assertEquals(200, responseCode, "Health check should return 200");
        assertTrue(responseBody.contains("{"), "Health check should return JSON");
        assertTrue(responseBody.contains("ok"), "Health check should return ok status");

        System.out.println("✓ Health check JSON format test passed");
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        java.io.InputStream inputStream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();

        if (inputStream == null) {
            return "";
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    /**
     * Get authentication token from the login endpoint
     *
     * @return JWT token string
     * @throws IOException if login fails
     */
    private String getAuthToken() throws IOException {
        String loginJson = """
            {
                "username": "admin",
                "password": "admin123"
            }
            """;

        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "/login").openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(loginJson.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, "Login should succeed");

        String responseBody = readResponse(connection);
        assertTrue(responseBody.contains("\"token\""), "Login response should contain token");

        // Extract token from JSON response
        String tokenPrefix = "\"token\":\"";
        int tokenStart = responseBody.indexOf(tokenPrefix);
        if (tokenStart != -1) {
            tokenStart += tokenPrefix.length();
            int tokenEnd = responseBody.indexOf("\"", tokenStart);
            if (tokenEnd != -1) {
                return responseBody.substring(tokenStart, tokenEnd);
            }
        }

        throw new IOException("Failed to extract token from login response");
    }
}
