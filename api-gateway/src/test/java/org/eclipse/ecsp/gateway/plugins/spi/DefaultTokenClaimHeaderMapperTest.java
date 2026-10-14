package org.eclipse.ecsp.gateway.plugins.spi;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefaultTokenClaimHeaderMapperTest {

    @Test
    void removesInboundMappedHeadersWhenClaimIsMissingAndUsesVerifiedClaimWhenPresent() {
        Claims claims = Jwts.claims().add("tenantId", "trusted-tenant").build();
        Map<String, String> mapping = Map.of(
                "tenantId", "tenantId",
                "accountId", "accountId");
        ClaimHeaderMappingContext context = new ClaimHeaderMappingContext(
                claims, mapping, "read", Set.of("read"));
        ServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("tenantId", "attacker-tenant")
                .header("accountId", "attacker-account")
                .build();
        ServerHttpRequest.Builder builder = request.mutate();

        ServerHttpRequest forwarded = new DefaultTokenClaimHeaderMapper().map(builder, context).build();

        assertEquals("trusted-tenant", forwarded.getHeaders().getFirst("tenantId"));
        assertNull(forwarded.getHeaders().getFirst("accountId"));
    }
}