package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.utils.PathExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PathExtractor.
 */
class PathExtractorTest {

    private PathExtractor pathExtractor;

    @BeforeEach
    void setUp() {
        pathExtractor = new PathExtractor();
    }

    @Test
    void testExtractServiceFromStandardPath() {
        String path = "/user-service/get-profile";

        String service = pathExtractor.extractService(path);
        String route = pathExtractor.extractRoute(path);

        assertThat(service).isEqualTo("user-service");
        assertThat(route).isEqualTo("get-profile");
    }

    @Test
    void testExtractServiceFromNestedRoutePath() {
        String path = "/vehicle-service/api/v1/vehicles";

        String service = pathExtractor.extractService(path);
        String route = pathExtractor.extractRoute(path);

        assertThat(service).isEqualTo("vehicle-service");
        assertThat(route).isEqualTo("api/v1/vehicles");
    }

    @Test
    void testExtractServiceOnly() {
        String path = "/healthz";

        String service = pathExtractor.extractService(path);
        String route = pathExtractor.extractRoute(path);

        assertThat(service).isEqualTo("healthz");
        assertThat(route).isEmpty();
    }

    @Test
    void testExtractWithoutLeadingSlash() {
        String path = "user-service/get-profile";

        String service = pathExtractor.extractService(path);
        String route = pathExtractor.extractRoute(path);

        assertThat(service).isEqualTo("user-service");
        assertThat(route).isEqualTo("get-profile");
    }

    @Test
    void testExtractWithTrailingSlash() {
        String path = "/user-service/get-profile/";

        String service = pathExtractor.extractService(path);
        String route = pathExtractor.extractRoute(path);

        assertThat(service).isEqualTo("user-service");
        assertThat(route).isEqualTo("get-profile/");
    }

    @Test
    void testExtractFromEmptyPath() {
        String path = "";

        String service = pathExtractor.extractService(path);
        String route = pathExtractor.extractRoute(path);

        assertThat(service).isEmpty();
        assertThat(route).isEmpty();
    }

    @Test
    void testExtractFromNullPath() {
        String path = null;

        String service = pathExtractor.extractService(path);
        String route = pathExtractor.extractRoute(path);

        assertThat(service).isEmpty();
        assertThat(route).isEmpty();
    }

    @Test
    void testExtractFromSingleSlash() {
        String path = "/";

        String service = pathExtractor.extractService(path);
        String route = pathExtractor.extractRoute(path);

        assertThat(service).isEmpty();
        assertThat(route).isEmpty();
    }

    @Test
    void testExtractWithQueryParameters() {
        String path = "/user-service/get-profile?id=123";

        String service = pathExtractor.extractService(path);
        String route = pathExtractor.extractRoute(path);

        assertThat(service).isEqualTo("user-service");
        assertThat(route).isEqualTo("get-profile?id=123");
    }

    @Test
    void testExtractComplexRoute() {
        String path = "/payment-service/transactions/2024/01/15/report";

        String service = pathExtractor.extractService(path);
        String route = pathExtractor.extractRoute(path);

        assertThat(service).isEqualTo("payment-service");
        assertThat(route).isEqualTo("transactions/2024/01/15/report");
    }
}
