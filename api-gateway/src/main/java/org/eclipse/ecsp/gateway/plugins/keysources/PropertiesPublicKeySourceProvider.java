package org.eclipse.ecsp.gateway.plugins.keysources;

import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Implementation of PublicKeySourceProvider that gets key sources from properties configuration.
 *
 * @author Abhishek Kumar
 */
@Component
public class PropertiesPublicKeySourceProvider implements PublicKeySourceProvider {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PropertiesPublicKeySourceProvider.class);
    private final JwtProperties jwtProperties;

    /**
     * Constructor with JWT properties dependency.
     *
     * @param jwtProperties the JWT configuration properties
     */
    public PropertiesPublicKeySourceProvider(JwtProperties jwtProperties) {
        LOGGER.info("PropertiesPublicKeySourceProvider initialized with JWT properties");
        this.jwtProperties = jwtProperties;
    }

    /**
     * Returns a list of public key sources from properties configuration.
     *
     * @return list of PublicKeySource from properties
     */
    @Override
    public List<PublicKeySource> keySources() {
        return jwtProperties.getKeySources() != null ? jwtProperties.getKeySources() : List.of();
    }
}
