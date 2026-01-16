package org.eclipse.ecsp.gateway.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Duration;

/**
 * Configuration class for a public key source.
 *
 * @author Abhishek Kumar
 */
@Setter
@Getter
@NoArgsConstructor
public class PublicKeySource {
    private String id;
    private boolean isDefault = false;
    private PublicKeyType type = PublicKeyType.JWKS;
    private String url;
    private String location;
    private PublicKeyAuthType authType = PublicKeyAuthType.NONE;
    private PublicKeyCredentials credentials;
    private Duration refreshInterval = Duration.ofDays(1);
    private String issuer;
    private boolean useProviderPrefixedKey = false;
}