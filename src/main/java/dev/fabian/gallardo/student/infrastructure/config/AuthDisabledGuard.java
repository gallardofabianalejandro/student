package dev.fabian.gallardo.student.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Falla el arranque si se apaga el login en production: una API abierta en produccion nunca es intencional. */
@Configuration
@ConditionalOnProperty(prefix = "app.auth", name = "enabled", havingValue = "false")
public class AuthDisabledGuard {

    private static final Logger LOG = LoggerFactory.getLogger(AuthDisabledGuard.class);

    public AuthDisabledGuard(Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("production", "prod"))) {
            throw new IllegalStateException(
                    "app.auth.enabled=false no se permite con el perfil production: el login no puede apagarse en produccion");
        }
        LOG.warn("LOGIN APAGADO (app.auth.enabled=false): toda la API esta abierta. Solo para desarrollo local.");
    }
}
