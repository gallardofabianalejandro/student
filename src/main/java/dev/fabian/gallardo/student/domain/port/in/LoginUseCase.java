package dev.fabian.gallardo.student.domain.port.in;

import dev.fabian.gallardo.student.domain.model.AuthTokens;

public interface LoginUseCase {

    AuthTokens login(String username, String rawPassword);
}
