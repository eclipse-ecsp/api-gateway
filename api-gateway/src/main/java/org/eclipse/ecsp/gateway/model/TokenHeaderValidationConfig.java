package org.eclipse.ecsp.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuration class for token header validation.
 *
 * @author Abhishek Kumar
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TokenHeaderValidationConfig {
    private boolean required;
    private String regex;
}
