package dev.fabian.gallardo.student.infrastructure.persistence;

import dev.fabian.gallardo.student.domain.exception.UsernameAlreadyExistsException;
import dev.fabian.gallardo.student.domain.model.AppUser;
import dev.fabian.gallardo.student.domain.model.Role;
import dev.fabian.gallardo.student.domain.port.out.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaUserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;
    private final EntityManager entityManager;

    JpaUserRepositoryAdapter(UserJpaRepository jpa, EntityManager entityManager) {
        this.jpa = jpa;
        this.entityManager = entityManager;
    }

    /**
     * Usa persist (no save/merge): con id derivado del username, merge actualizaría en silencio al usuario
     * existente y pisaría su contraseña si dos registros concurrentes pasan la verificación previa.
     */
    @Override
    @Transactional
    public AppUser save(AppUser user) {
        try {
            entityManager.persist(toEntity(user));
            entityManager.flush();
        } catch (PersistenceException e) {
            throw new UsernameAlreadyExistsException(user.username());
        }
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> findByUsername(String username) {
        return jpa.findByUsername(username).map(JpaUserRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }

    private static UserEntity toEntity(AppUser user) {
        String roles = user.roles().stream().map(Role::name).sorted().collect(Collectors.joining(","));
        return new UserEntity(user.id(), user.username(), user.passwordHash(), roles);
    }

    private static AppUser toDomain(UserEntity entity) {
        Set<Role> roles = Arrays.stream(entity.getRoles().split(","))
                .filter(r -> !r.isBlank())
                .map(Role::valueOf)
                .collect(Collectors.toSet());
        return new AppUser(entity.getId(), entity.getUsername(), entity.getPasswordHash(), roles);
    }
}
