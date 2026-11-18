package dev.fabian.gallardo.student.domain.port.in;

import dev.fabian.gallardo.student.domain.model.AuthTokens;

public interface RefreshTokenUseCase {

    AuthTokens refresh(String refreshToken);
}
