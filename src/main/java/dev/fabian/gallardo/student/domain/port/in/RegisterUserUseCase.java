package dev.fabian.gallardo.student.domain.port.in;

import dev.fabian.gallardo.student.domain.model.AppUser;

public interface RegisterUserUseCase {

    AppUser register(String username, String rawPassword);
}
