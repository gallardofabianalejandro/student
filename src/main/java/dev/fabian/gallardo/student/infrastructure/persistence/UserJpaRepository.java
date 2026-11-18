package dev.fabian.gallardo.student.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
