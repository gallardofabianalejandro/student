package dev.fabian.gallardo.student.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nacionservicios.emv.persistence.autoconfigure.PersistenceAutoConfiguration;
import dev.fabian.gallardo.student.TestcontainersConfig;
import dev.fabian.gallardo.student.domain.model.SampleItem;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** Slice JPA: solo el repositorio Spring Data contra PostgreSQL real, sin controllers ni use cases. Cada test hace rollback. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ImportAutoConfiguration(PersistenceAutoConfiguration.class)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class SampleItemJpaRepositoryTest {

    private final SampleItemJpaRepository repository;
    private final TestEntityManager entityManager;

    @Autowired
    SampleItemJpaRepositoryTest(SampleItemJpaRepository repository, TestEntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    private static SampleItemEntity entity(String name) {
        return SampleItemEntity.from(new SampleItem(UUID.randomUUID(), name, null, 2, new BigDecimal("9.50")));
    }

    @Test
    void findByName_whenSaved_thenReturnsTheDomainItem() {
        var saved = repository.saveAndFlush(entity("lapiz"));
        entityManager.clear();

        var found = repository.findByName("lapiz");

        assertThat(found).isPresent();
        assertThat(found.get().toDomain().id()).isEqualTo(saved.toDomain().id());
        assertThat(found.get().toDomain().description()).isNull();
    }

    @Test
    void saveAndFlush_whenNameRepeated_thenViolatesUniqueConstraint() {
        repository.saveAndFlush(entity("repetido"));

        assertThatThrownBy(() -> repository.saveAndFlush(entity("repetido")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
