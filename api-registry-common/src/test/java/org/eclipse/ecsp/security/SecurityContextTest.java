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

package org.eclipse.ecsp.security;

import org.eclipse.ecsp.tokenvalidator.model.TokenClaim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link SecurityContext}.
 */
@ExtendWith(MockitoExtension.class)
class SecurityContextTest {

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    @Test
    void shouldStoreAndRetrieveToken() {
        List<TokenClaim> claims = Collections.emptyList();
        SecurityContext.set("my-token", claims);

        Optional<String> token = SecurityContext.getToken();
        Assertions.assertTrue(token.isPresent());
        Assertions.assertEquals("my-token", token.get());
    }

    @Test
    void shouldReturnEmptyWhenNoToken() {
        Optional<String> token = SecurityContext.getToken();
        Assertions.assertFalse(token.isPresent());
    }

    @Test
    void shouldParseExpiryFromExpClaim() {
        long futureEpoch = Instant.parse("2099-01-01T00:00:00Z").getEpochSecond();
        List<TokenClaim> claims = Collections.singletonList(new TokenClaim("exp", futureEpoch));
        SecurityContext.set("token", claims);

        Assertions.assertFalse(SecurityContext.isTokenExpired());
    }

    @Test
    void shouldParseSpaceSeparatedScopes() {
        List<TokenClaim> claims = Collections.singletonList(new TokenClaim("scope", "read write admin"));
        SecurityContext.set("token", claims);

        Set<String> scopes = SecurityContext.getScopes();
        Assertions.assertTrue(scopes.contains("read"));
        Assertions.assertTrue(scopes.contains("write"));
        Assertions.assertTrue(scopes.contains("admin"));
        Assertions.assertEquals(3, scopes.size());
    }

    @Test
    void shouldParseArrayScopes() {
        List<String> scopeList = Arrays.asList("read", "write");
        List<TokenClaim> claims = Collections.singletonList(new TokenClaim("scope", scopeList));
        SecurityContext.set("token", claims);

        Set<String> scopes = SecurityContext.getScopes();
        Assertions.assertTrue(scopes.contains("read"));
        Assertions.assertTrue(scopes.contains("write"));
        Assertions.assertEquals(2, scopes.size());
    }

    @Test
    void shouldDetectExpiredToken() {
        long pastEpoch = Instant.parse("2000-01-01T00:00:00Z").getEpochSecond();
        List<TokenClaim> claims = Collections.singletonList(new TokenClaim("exp", pastEpoch));
        SecurityContext.set("token", claims);

        Assertions.assertTrue(SecurityContext.isTokenExpired());
    }

    @Test
    void shouldClearContext() {
        List<TokenClaim> claims = Collections.emptyList();
        SecurityContext.set("token", claims);
        SecurityContext.clear();

        Assertions.assertFalse(SecurityContext.getToken().isPresent());
        Assertions.assertTrue(SecurityContext.getClaims().isEmpty());
        Assertions.assertFalse(SecurityContext.getUserId().isPresent());
        Assertions.assertTrue(SecurityContext.getScopes().isEmpty());
        Assertions.assertTrue(SecurityContext.isTokenExpired());
    }

    @Test
    void shouldIsolateThreads() throws InterruptedException {
        List<TokenClaim> mainClaims = Collections.singletonList(new TokenClaim("sub", "main-user"));
        SecurityContext.set("main-token", mainClaims);

        AtomicReference<Optional<String>> threadToken = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            threadToken.set(SecurityContext.getToken());
            latch.countDown();
        });
        thread.start();
        latch.await();

        Assertions.assertFalse(threadToken.get().isPresent());
        Assertions.assertTrue(SecurityContext.getToken().isPresent());
        Assertions.assertEquals("main-token", SecurityContext.getToken().get());
    }

    @Test
    void shouldExtractUserId() {
        List<TokenClaim> claims = Collections.singletonList(new TokenClaim("sub", "user-123"));
        SecurityContext.set("token", claims);

        Optional<String> userId = SecurityContext.getUserId();
        Assertions.assertTrue(userId.isPresent());
        Assertions.assertEquals("user-123", userId.get());
    }

    @Test
    void shouldReturnEmptyUserIdWhenNoSubClaim() {
        List<TokenClaim> claims = Collections.emptyList();
        SecurityContext.set("token", claims);

        Assertions.assertFalse(SecurityContext.getUserId().isPresent());
    }

    @Test
    void shouldReturnTrueForIsTokenExpiredWhenNoContextSet() {
        Assertions.assertTrue(SecurityContext.isTokenExpired());
    }

    @Test
    void shouldExtractTenantId() {
        List<TokenClaim> claims = Collections.singletonList(new TokenClaim("tenantId", "tenant-abc"));
        SecurityContext.set("token", claims);

        Optional<String> tenantId = SecurityContext.getTenantId();
        Assertions.assertTrue(tenantId.isPresent());
        Assertions.assertEquals("tenant-abc", tenantId.get());
    }

    @Test
    void shouldReturnEmptyTenantIdWhenNoTenantIdClaim() {
        List<TokenClaim> claims = Collections.emptyList();
        SecurityContext.set("token", claims);

        Assertions.assertFalse(SecurityContext.getTenantId().isPresent());
    }

    @Test
    void shouldExtractAccountId() {
        List<TokenClaim> claims = Collections.singletonList(new TokenClaim("accountId", "account-xyz"));
        SecurityContext.set("token", claims);

        Optional<String> accountId = SecurityContext.getAccountId();
        Assertions.assertTrue(accountId.isPresent());
        Assertions.assertEquals("account-xyz", accountId.get());
    }

    @Test
    void shouldReturnEmptyAccountIdWhenNoAccountIdClaim() {
        List<TokenClaim> claims = Collections.emptyList();
        SecurityContext.set("token", claims);

        Assertions.assertFalse(SecurityContext.getAccountId().isPresent());
    }

    @Test
    void shouldExtractBothTenantIdAndAccountId() {
        List<TokenClaim> claims = Arrays.asList(
            new TokenClaim("tenantId", "tenant-42"),
            new TokenClaim("accountId", "account-99")
        );
        SecurityContext.set("token", claims);

        Assertions.assertEquals("tenant-42", SecurityContext.getTenantId().orElse(null));
        Assertions.assertEquals("account-99", SecurityContext.getAccountId().orElse(null));
    }

    @Test
    void shouldPreferUserIdClaimOverSubClaim() {
        List<TokenClaim> claims = Arrays.asList(
            new TokenClaim("sub", "sub-user"),
            new TokenClaim("user_id", "explicit-user-id")
        );
        SecurityContext.set("token", claims);

        Optional<String> userId = SecurityContext.getUserId();
        Assertions.assertTrue(userId.isPresent());
        Assertions.assertEquals("explicit-user-id", userId.get());
    }

    @Test
    void shouldClearTenantIdAndAccountIdOnClear() {
        List<TokenClaim> claims = Arrays.asList(
            new TokenClaim("tenantId", "tenant-clear"),
            new TokenClaim("accountId", "account-clear")
        );
        SecurityContext.set("token", claims);
        SecurityContext.clear();

        Assertions.assertFalse(SecurityContext.getTenantId().isPresent());
        Assertions.assertFalse(SecurityContext.getAccountId().isPresent());
    }
}
