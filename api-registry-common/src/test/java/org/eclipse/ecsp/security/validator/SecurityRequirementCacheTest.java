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

package org.eclipse.ecsp.security.validator;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.eclipse.ecsp.interceptors.SecurityRequirementCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Tests for {@link SecurityRequirementCache}.
 */
@ExtendWith(MockitoExtension.class)
class SecurityRequirementCacheTest {

    private SecurityRequirementCache underTest;

    @BeforeEach
    void beforeEach() {
        underTest = new SecurityRequirementCache();
    }

    @Test
    void shouldReturnFalseWhenAnnotationAbsent() {
        HandlerMethod handlerMethod = Mockito.mock(HandlerMethod.class);
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class)).thenReturn(null);

        boolean result = underTest.isSecured(handlerMethod);

        Assertions.assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenAnnotationPresent() throws Exception {
        Method method = SampleController.class.getMethod("securedEndpoint");
        SecurityRequirement annotation = method.getAnnotation(SecurityRequirement.class);

        HandlerMethod handlerMethod = Mockito.mock(HandlerMethod.class);
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class)).thenReturn(annotation);

        boolean result = underTest.isSecured(handlerMethod);

        Assertions.assertTrue(result);
    }

    @Test
    void shouldReturnCachedResultOnSecondCall() {
        HandlerMethod handlerMethod = Mockito.mock(HandlerMethod.class);
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class)).thenReturn(null);

        underTest.isSecured(handlerMethod);
        underTest.isSecured(handlerMethod);

        // getMethodAnnotation should be called exactly once due to caching
        Mockito.verify(handlerMethod, Mockito.times(1)).getMethodAnnotation(SecurityRequirement.class);
    }

    @Test
    void shouldHandleConcurrentFirstCalls() throws Exception {
        HandlerMethod handlerMethod = Mockito.mock(HandlerMethod.class);
        Mockito.when(handlerMethod.getMethodAnnotation(SecurityRequirement.class)).thenReturn(null);

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                return underTest.isSecured(handlerMethod);
            }));
        }
        startLatch.countDown();

        for (Future<Boolean> future : futures) {
            Assertions.assertFalse(future.get());
        }
        executor.shutdown();
    }

    static class SampleController {

        /** Public endpoint with no security annotation. */
        public void publicEndpoint() {
            // sample
        }

        /** Secured endpoint with @SecurityRequirement annotation. */
        @SecurityRequirement(name = "bearerAuth")
        public void securedEndpoint() {
            // sample
        }
    }
}
