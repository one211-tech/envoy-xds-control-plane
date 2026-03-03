package com.one211.xds;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XdsConfigManager (Simplified version without external dependencies)
 */
class XdsConfigManagerSimpleTest {

    @Test
    void testConfigManagerCreation() {
        XdsConfigManager configManager = new XdsConfigManager();
        assertNotNull(configManager, "ConfigManager should not be null");
    }

    @Test
    void testEndpointConfig() {
        XdsConfigManager.EndpointConfig endpoint =
            new XdsConfigManager.EndpointConfig("test_cluster", "test-host", 8080);

        assertEquals("test_cluster", endpoint.getClusterName());
        assertEquals("test-host", endpoint.getHost());
        assertEquals(8080, endpoint.getPort());
    }

    @Test
    void testSnapshotGeneration() {
        XdsConfigManager configManager = new XdsConfigManager();
        // Envoy libraries are now available, so snapshot generation should work
        assertDoesNotThrow(() -> configManager.generateInitialSnapshot(),
                          "Snapshot generation should work with Envoy libraries");
    }

    @Test
    void testAddCluster() {
        XdsConfigManager configManager = new XdsConfigManager();
        String clusterJson = """
            {
                "name": "test_cluster",
                "type": "STRICT_DNS",
                "address": "test-host",
                "port": 8080
            }
            """;

        assertDoesNotThrow(() -> configManager.addCluster(clusterJson),
                          "addCluster should not throw exception");
    }

    @Test
    void testUpdateEndpoints() {
        XdsConfigManager configManager = new XdsConfigManager();
        String endpointsJson = """
            {
                "clusterName": "test_cluster",
                "endpoints": [
                    {"host": "test-host1", "port": 8080},
                    {"host": "test-host2", "port": 8080}
                ]
            }
            """;

        assertDoesNotThrow(() -> configManager.updateEndpoints(endpointsJson),
                          "updateEndpoints should not throw exception");
    }

    @Test
    void testFileTimestampsInitiallyEmpty() {
        XdsConfigManager configManager = new XdsConfigManager();
        try {
            java.lang.reflect.Field field = XdsConfigManager.class.getDeclaredField("fileTimestamps");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, java.nio.file.attribute.FileTime> timestamps =
                (Map<String, java.nio.file.attribute.FileTime>) field.get(configManager);

            assertNotNull(timestamps, "File timestamps map should not be null");
            assertTrue(timestamps.isEmpty(), "File timestamps should be empty initially");
        } catch (Exception e) {
            fail("Failed to access fileTimestamps field: " + e.getMessage());
        }
    }

    @Test
    void testDynamicEndpointsInitiallyEmpty() {
        XdsConfigManager configManager = new XdsConfigManager();
        try {
            java.lang.reflect.Field field = XdsConfigManager.class.getDeclaredField("dynamicEndpoints");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, List<XdsConfigManager.EndpointConfig>> endpoints =
                (Map<String, List<XdsConfigManager.EndpointConfig>>) field.get(configManager);

            assertNotNull(endpoints, "Dynamic endpoints map should not be null");
            assertTrue(endpoints.isEmpty(), "Dynamic endpoints should be empty initially");
        } catch (Exception e) {
            fail("Failed to access dynamicEndpoints field: " + e.getMessage());
        }
    }

    @Test
    void testHasConfigurationChanged() {
        XdsConfigManager configManager = new XdsConfigManager();
        assertDoesNotThrow(configManager::hasConfigurationChanged,
                          "hasConfigurationChanged should not throw exception");
    }
}
