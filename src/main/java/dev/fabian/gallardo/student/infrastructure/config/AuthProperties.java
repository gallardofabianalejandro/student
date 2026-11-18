package dev.fabian.gallardo.student.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code secureCookie}: la cookie del refresh token solo viaja por HTTPS (false únicamente en local/test).
 * {@code registrationEnabled}: habilita POST /api/auth/register (cerrado por defecto fuera de local).
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        @DefaultValue("true") boolean secureCookie,
        @DefaultValue("false") boolean registrationEnabled) {
}
