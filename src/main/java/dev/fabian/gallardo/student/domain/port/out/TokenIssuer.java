package dev.fabian.gallardo.student.domain.port.out;

import dev.fabian.gallardo.student.domain.model.AppUser;

import java.time.Duration;
import java.util.Optional;

/** Puerto de salida hacia el mecanismo de tokens (hoy JWT). El dominio no conoce la librería. */
public interface TokenIssuer {

    String issueAccessToken(AppUser user);

    String issueRefreshToken(AppUser user);

    /** Username dueño del refresh token, o vacío si es inválido, vencido o no es un refresh token. */
    Optional<String> usernameOfRefreshToken(String refreshToken);

    Duration refreshTtl();
}
