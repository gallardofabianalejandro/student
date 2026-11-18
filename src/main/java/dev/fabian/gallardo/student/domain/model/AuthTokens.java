package dev.fabian.gallardo.student.domain.model;

import java.time.Duration;

/** Par de tokens emitido al autenticar. {@code refreshTtl} define la vida de la cookie del refresh token. */
public record AuthTokens(String accessToken, String refreshToken, Duration refreshTtl) {
}
