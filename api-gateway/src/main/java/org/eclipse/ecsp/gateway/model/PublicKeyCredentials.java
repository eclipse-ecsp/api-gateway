package org.eclipse.ecsp.gateway.model;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Configuration class for public key authentication credentials.
 *
 * @author Abhishek Kumar
 */
@Getter
@Setter
public class PublicKeyCredentials {
    private String username;
    private String password;
    private String clientId;
    private String clientSecret;
    private List<String> scopes;
    private String tokenEndpoint;
}
