package org.eclipse.ecsp.gateway.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test purpose    - Verify PathExtractor service and route extraction logic.
 * Test data       - Various path formats.
 * Test expected   - Correct service and route extraction.
 * Test type       - Positive and Negative.
 */
class PathExtractorTest {

    private PathExtractor pathExtractor;

    @BeforeEach
    void setUp() {
        pathExtractor = new PathExtractor();
    }

    /**
     * Test purpose          - Verify extractService with standard path.
     * Test data             - Path "/user-service/get-profile".
     * Test expected result  - Returns "user-service".
     * Test type             - Positive.
     */
    @Test
    void extractService_StandardPath_ReturnsServiceName() {
        // GIVEN: Standard path with service and route
        String path = "/user-service/get-profile";

        // WHEN: Service is extracted
        String service = pathExtractor.extractService(path);

        // THEN: Should return service name
        assertEquals("user-service", service);
    }

    /**
     * Test purpose          - Verify extractRoute with standard path.
     * Test data             - Path "/user-service/get-profile".
     * Test expected result  - Returns "get-profile".
     * Test type             - Positive.
     */
    @Test
    void extractRoute_StandardPath_ReturnsRoutePath() {
        // GIVEN: Standard path with service and route
        String path = "/user-service/get-profile";

        // WHEN: Route is extracted
        String route = pathExtractor.extractRoute(path);

        // THEN: Should return route path
        assertEquals("get-profile", route);
    }

    /**
     * Test purpose          - Verify extractService with nested route path.
     * Test data             - Path "/vehicle-service/api/v1/vehicles".
     * Test expected result  - Returns "vehicle-service".
     * Test type             - Positive.
     */
    @Test
    void extractService_NestedRoutePath_ReturnsServiceName() {
        // GIVEN: Path with nested route
        String path = "/vehicle-service/api/v1/vehicles";

        // WHEN: Service is extracted
        String service = pathExtractor.extractService(path);

        // THEN: Should return service name
        assertEquals("vehicle-service", service);
    }

    /**
     * Test purpose          - Verify extractRoute with nested route path.
     * Test data             - Path "/vehicle-service/api/v1/vehicles".
     * Test expected result  - Returns "api/v1/vehicles".
     * Test type             - Positive.
     */
    @Test
    void extractRoute_NestedRoutePath_ReturnsFullRoute() {
        // GIVEN: Path with nested route
        String path = "/vehicle-service/api/v1/vehicles";

        // WHEN: Route is extracted
        String route = pathExtractor.extractRoute(path);

        // THEN: Should return full route path
        assertEquals("api/v1/vehicles", route);
    }

    /**
     * Test purpose          - Verify extractService with service-only path.
     * Test data             - Path "/healthz".
     * Test expected result  - Returns "healthz".
     * Test type             - Positive.
     */
    @Test
    void extractService_ServiceOnlyPath_ReturnsServiceName() {
        // GIVEN: Path with only service name
        String path = "/healthz";

        // WHEN: Service is extracted
        String service = pathExtractor.extractService(path);

        // THEN: Should return entire path as service
        assertEquals("healthz", service);
    }

    /**
     * Test purpose          - Verify extractRoute with service-only path.
     * Test data             - Path "/healthz".
     * Test expected result  - Returns empty string.
     * Test type             - Positive.
     */
    @Test
    void extractRoute_ServiceOnlyPath_ReturnsEmptyString() {
        // GIVEN: Path with only service name
        String path = "/healthz";

        // WHEN: Route is extracted
        String route = pathExtractor.extractRoute(path);

        // THEN: Should return empty string
        assertEquals("", route);
    }

    /**
     * Test purpose          - Verify extractService handles null path.
     * Test data             - Null value.
     * Test expected result  - Returns empty string.
     * Test type             - Negative.
     */
    @Test
    void extractService_NullPath_ReturnsEmptyString() {
        // GIVEN: Null path
        String path = null;

        // WHEN: Service is extracted
        String service = pathExtractor.extractService(path);

        // THEN: Should return empty string
        assertEquals("", service);
    }

    /**
     * Test purpose          - Verify extractRoute handles null path.
     * Test data             - Null value.
     * Test expected result  - Returns empty string.
     * Test type             - Negative.
     */
    @Test
    void extractRoute_NullPath_ReturnsEmptyString() {
        // GIVEN: Null path
        String path = null;

        // WHEN: Route is extracted
        String route = pathExtractor.extractRoute(path);

        // THEN: Should return empty string
        assertEquals("", route);
    }

    /**
     * Test purpose          - Verify extractService handles empty path.
     * Test data             - Empty string.
     * Test expected result  - Returns empty string.
     * Test type             - Negative.
     */
    @Test
    void extractService_EmptyPath_ReturnsEmptyString() {
        // GIVEN: Empty path
        String path = "";

        // WHEN: Service is extracted
        String service = pathExtractor.extractService(path);

        // THEN: Should return empty string
        assertEquals("", service);
    }

    /**
     * Test purpose          - Verify extractRoute handles empty path.
     * Test data             - Empty string.
     * Test expected result  - Returns empty string.
     * Test type             - Negative.
     */
    @Test
    void extractRoute_EmptyPath_ReturnsEmptyString() {
        // GIVEN: Empty path
        String path = "";

        // WHEN: Route is extracted
        String route = pathExtractor.extractRoute(path);

        // THEN: Should return empty string
        assertEquals("", route);
    }

    /**
     * Test purpose          - Verify extractService handles blank path.
     * Test data             - Whitespace only string.
     * Test expected result  - Returns empty string.
     * Test type             - Negative.
     */
    @Test
    void extractService_BlankPath_ReturnsEmptyString() {
        // GIVEN: Blank path
        String path = "   ";

        // WHEN: Service is extracted
        String service = pathExtractor.extractService(path);

        // THEN: Should return empty string
        assertEquals("", service);
    }

    /**
     * Test purpose          - Verify extractService handles path without leading slash.
     * Test data             - Path "user-service/profile".
     * Test expected result  - Returns "user-service".
     * Test type             - Positive.
     */
    @Test
    void extractService_PathWithoutLeadingSlash_ReturnsServiceName() {
        // GIVEN: Path without leading slash
        String path = "user-service/profile";

        // WHEN: Service is extracted
        String service = pathExtractor.extractService(path);

        // THEN: Should return service name
        assertEquals("user-service", service);
    }

    /**
     * Test purpose          - Verify extractRoute handles path without leading slash.
     * Test data             - Path "user-service/profile".
     * Test expected result  - Returns "profile".
     * Test type             - Positive.
     */
    @Test
    void extractRoute_PathWithoutLeadingSlash_ReturnsRoutePath() {
        // GIVEN: Path without leading slash
        String path = "user-service/profile";

        // WHEN: Route is extracted
        String route = pathExtractor.extractRoute(path);

        // THEN: Should return route path
        assertEquals("profile", route);
    }

    /**
     * Test purpose          - Verify extractService with multiple slashes in route.
     * Test data             - Path "/api/v1/users/123".
     * Test expected result  - Returns "api".
     * Test type             - Positive.
     */
    @Test
    void extractService_MultipleSlashesInRoute_ReturnsFirstSegment() {
        // GIVEN: Path with multiple slashes
        String path = "/api/v1/users/123";

        // WHEN: Service is extracted
        String service = pathExtractor.extractService(path);

        // THEN: Should return first segment only
        assertEquals("api", service);
    }

    /**
     * Test purpose          - Verify extractRoute with multiple slashes in route.
     * Test data             - Path "/api/v1/users/123".
     * Test expected result  - Returns "v1/users/123".
     * Test type             - Positive.
     */
    @Test
    void extractRoute_MultipleSlashesInRoute_ReturnsFullRoute() {
        // GIVEN: Path with multiple slashes
        String path = "/api/v1/users/123";

        // WHEN: Route is extracted
        String route = pathExtractor.extractRoute(path);

        // THEN: Should return everything after first slash
        assertEquals("v1/users/123", route);
    }
}
