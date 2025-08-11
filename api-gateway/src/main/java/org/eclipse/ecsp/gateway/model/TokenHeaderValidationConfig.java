package org.eclipse.ecsp.gateway.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration class for token header validation.
 *
 * @author Abhishek Kumar
 */
@Setter
@Getter
public class TokenHeaderValidationConfig {
    private boolean required;
    private String regex;
}
