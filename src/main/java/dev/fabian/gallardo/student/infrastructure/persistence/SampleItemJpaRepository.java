package dev.fabian.gallardo.student.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleItemJpaRepository extends JpaRepository<SampleItemEntity, UUID> {

    Optional<SampleItemEntity> findByName(String name);
}
