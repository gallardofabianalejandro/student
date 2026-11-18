package dev.fabian.gallardo.student.domain.port.out;

import dev.fabian.gallardo.student.domain.model.AppUser;

import java.util.Optional;

/** Puerto de salida -- implementación JPA en infrastructure/persistence/. */
public interface UserRepository {

    /**
     * Inserta un usuario nuevo; nunca pisa uno existente.
     *
     * @throws dev.fabian.gallardo.student.domain.exception.UsernameAlreadyExistsException si el username o el id ya existen
     */
    AppUser save(AppUser user);

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
