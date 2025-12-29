/********************************************************************************
 * Copyright (c) 2023-24 Harman International
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.gateway.performance;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.eclipse.ecsp.gateway.model.PublicKeyInfo;
import org.eclipse.ecsp.gateway.model.TokenHeaderValidationConfig;
import org.eclipse.ecsp.gateway.service.PublicKeyService;
import org.eclipse.ecsp.gateway.utils.JwtTestTokenGenerator;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Performance test AFTER optimization.
 * This test uses the OPTIMIZED approach with Pattern caching (like the real JwtAuthFilter now does).
 * Compare results with JwtAuthFilterPerformanceTest to see the improvement!
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthFilter AFTER Optimization Performance Test")
class JwtAuthFilterOptimizedPerformanceTest {

    private static final IgniteLogger LOGGER =
            IgniteLoggerFactory.getLogger(JwtAuthFilterOptimizedPerformanceTest.class);
    private static final int WARMUP_ITERATIONS = 500;
    private static final int MEASUREMENT_ITERATIONS = 50_000;
    private static final int NUMBER_OF_RUNS = 5;
    private static final int NUMBER_SEVEN = 7;
    private static final double NANOSECONDS_TO_MILLISECONDS = 1_000_000.0;

    /**
     * OPTIMIZATION: Pattern cache - just like in the optimized JwtAuthFilter!.
     */
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    @Mock
    private PublicKeyService publicKeyService;

    @Mock
    private GatewayFilterChain filterChain;

    private JwtProperties jwtProperties;
    private String testToken;
    private PublicKey testPublicKey;

    @BeforeEach
    void setUp() {
        testPublicKey = JwtTestTokenGenerator.getTestPublicKey();
        testToken = JwtTestTokenGenerator.createDefaultToken();
        if (testToken.startsWith("Bearer ")) {
            testToken = testToken.substring(NUMBER_SEVEN);
        }

        jwtProperties = new JwtProperties();
        
        Map<String, TokenHeaderValidationConfig> headerValidation = new HashMap<>();
        
        TokenHeaderValidationConfig userIdConfig = new TokenHeaderValidationConfig();
        userIdConfig.setRequired(true);
        userIdConfig.setRegex("^[a-zA-Z0-9-_]+$");
        headerValidation.put("sub", userIdConfig);
        
        TokenHeaderValidationConfig emailConfig = new TokenHeaderValidationConfig();
        emailConfig.setRequired(true);
        emailConfig.setRegex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        headerValidation.put("email", emailConfig);
        
        TokenHeaderValidationConfig tenantConfig = new TokenHeaderValidationConfig();
        tenantConfig.setRequired(true);
        tenantConfig.setRegex("^tenant_[0-9]{3,10}$");
        headerValidation.put("tenantId", tenantConfig);
        
        jwtProperties.setTokenHeaderValidationConfig(headerValidation);
        jwtProperties.setTokenClaimToHeaderMapping(new HashMap<>());

        PublicKeyInfo publicKeyInfo = new PublicKeyInfo();
        publicKeyInfo.setKid("test-key-id");
        publicKeyInfo.setPublicKey(testPublicKey);
        publicKeyInfo.setSourceId("test-source");
        
        org.mockito.Mockito.lenient()
            .when(publicKeyService.findPublicKey(anyString(), anyString()))
            .thenReturn(Optional.of(publicKeyInfo));
    }

    /**
     * Main performance test with OPTIMIZATION (Pattern caching).
     * This simulates the OPTIMIZED JwtAuthFilter behavior.
     */
    @Test
    @DisplayName("Measure OPTIMIZED Performance with Pattern Caching")
    void testOptimizedPerformance() {
        LOGGER.info("============================================================");
        LOGGER.info("AFTER OPTIMIZATION - Pattern Caching Enabled");
        LOGGER.info("============================================================");
        LOGGER.info("Testing OPTIMIZED approach with PATTERN_CACHE");
        LOGGER.info("This is what JwtAuthFilter does NOW after optimization");
        LOGGER.info("Warmup: " + WARMUP_ITERATIONS + " iterations");
        LOGGER.info("Measurement: " + MEASUREMENT_ITERATIONS + " iterations");

        long totalTime = 0;

        for (int run = 1; run <= NUMBER_OF_RUNS; run++) {
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                executeOptimizedValidation();
            }

            // Measurement
            long startTime = System.nanoTime();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                executeOptimizedValidation();
            }
            long endTime = System.nanoTime();

            long runTime = (endTime - startTime);
            double runTimeMs = runTime / NANOSECONDS_TO_MILLISECONDS;
            double avgTimePerCallNs = (double) runTime / MEASUREMENT_ITERATIONS;

            totalTime += runTime;

            LOGGER.info(String.format("Run %d: Total: %8.2f ms | Avg per request: %8.2f ns%n",
                    run, runTimeMs, avgTimePerCallNs));
        }

        double avgTotalTimeMs = (totalTime / NUMBER_OF_RUNS) / NANOSECONDS_TO_MILLISECONDS;
        double avgTimePerRequestNs = (double) totalTime / (NUMBER_OF_RUNS * MEASUREMENT_ITERATIONS);

        LOGGER.info("=== AFTER OPTIMIZATION Results ===");
        LOGGER.info(String.format("Average total time: %.2f ms%n", avgTotalTimeMs));
        LOGGER.info(String.format("Average time per request: %.2f ns%n", avgTimePerRequestNs));
        LOGGER.info("===================================");
        LOGGER.info("COMPARE WITH BEFORE:");
        LOGGER.info("- BEFORE: ~68.24 ms total, ~1364.74 ns per request");
        LOGGER.info("- AFTER:  (see above results)");
        LOGGER.info("Calculate improvement:");
        LOGGER.info("- Improvement factor = BEFORE / AFTER");
        LOGGER.info("- E.g., if AFTER is 10ms: 68.24 / 10 = 6.8x faster!");
        LOGGER.info("============================================================");
    }

    private void executeOptimizedValidation() {
        try {
            Claims claims = Jwts.claims()
                    .add("sub", "test-user-123")
                    .add("email", "test@example.com")
                    .add("tenantId", "tenant_12345")
                    .add("scope", "read,write")
                    .build();
            
            testOptimizedRegexValidation(claims);
            
        } catch (Exception e) {
            // Ignore for performance testing
        }
    }

    /**
     * OPTIMIZED regex validation with Pattern caching.
     * This is what the OPTIMIZED JwtAuthFilter does now!
     */
    private void testOptimizedRegexValidation(Claims claims) {
        Map<String, TokenHeaderValidationConfig> configs = jwtProperties.getTokenHeaderValidationConfig();
        
        for (Map.Entry<String, TokenHeaderValidationConfig> entry : configs.entrySet()) {
            String headerName = entry.getKey();
            TokenHeaderValidationConfig config = entry.getValue();
            
            if (config.isRequired() && config.getRegex() != null) {
                String value = (String) claims.get(headerName);
                
                // OPTIMIZATION: Use cached Pattern instead of compiling every time!
                // This is exactly what JwtAuthFilter line 299-300 does now:
                Pattern pattern = PATTERN_CACHE.computeIfAbsent(config.getRegex(), Pattern::compile);
                pattern.matcher(value).matches();
            }
        }
    }

    /**
     * Isolated test that measures ONLY the optimized regex compilation.
     */
    @Test
    @DisplayName("Measure ISOLATED Optimized Regex with Cache")
    void testIsolatedOptimizedRegex() {
        LOGGER.info("============================================================");
        LOGGER.info("ISOLATED Optimized Regex Test with Pattern Cache");
        LOGGER.info("============================================================");
        LOGGER.info("Measuring optimized Pattern lookup (cache hit)");

        String[] regexPatterns = {
            "^[a-zA-Z0-9-_]+$",
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            "^tenant_[0-9]{3,10}$"
        };
        
        String[] testValues = {
            "test-user-123",
            "test@example.com",
            "tenant_12345"
        };

        // Pre-populate cache (simulate first request)
        for (String regex : regexPatterns) {
            PATTERN_CACHE.computeIfAbsent(regex, Pattern::compile);
        }

        long totalTime = 0;

        for (int run = 1; run <= NUMBER_OF_RUNS; run++) {
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                for (int j = 0; j < regexPatterns.length; j++) {
                    // OPTIMIZED: Use cached pattern
                    Pattern pattern = PATTERN_CACHE.get(regexPatterns[j]);
                    pattern.matcher(testValues[j]).matches();
                }
            }

            // Measurement
            long startTime = System.nanoTime();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                // OPTIMIZED: This is what JwtAuthFilter does NOW
                for (int j = 0; j < regexPatterns.length; j++) {
                    Pattern pattern = PATTERN_CACHE.get(regexPatterns[j]);
                    pattern.matcher(testValues[j]).matches();
                }
            }
            long endTime = System.nanoTime();

            long runTime = (endTime - startTime);
            double runTimeMs = runTime / NANOSECONDS_TO_MILLISECONDS;
            double avgTimePerCallNs = (double) runTime / MEASUREMENT_ITERATIONS;

            totalTime += runTime;

            LOGGER.info(String.format("Run %d: Total: %8.2f ms | Avg per request: %8.2f ns%n",
                    run, runTimeMs, avgTimePerCallNs));
        }

        double avgTotalTimeMs = (totalTime / NUMBER_OF_RUNS) / NANOSECONDS_TO_MILLISECONDS;
        double avgTimePerRequestNs = (double) totalTime / (NUMBER_OF_RUNS * MEASUREMENT_ITERATIONS);

        LOGGER.info("=== Isolated AFTER OPTIMIZATION Results ===");
        LOGGER.info(String.format("Average total time: %.2f ms%n", avgTotalTimeMs));
        LOGGER.info(String.format("Average time per request: %.2f ns%n", avgTimePerRequestNs));
        LOGGER.info("============================================");
        LOGGER.info("SAVE THESE RESULTS as 'AFTER optimization'!");
        LOGGER.info("============================================================");
        assertTrue(avgTotalTimeMs > 0, "Average total time should be positive");
        assertTrue(avgTimePerRequestNs > 0, "Average time per request should be positive");
        
    }
}



