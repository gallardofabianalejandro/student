package dev.fabian.gallardo.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.fabian.gallardo.student.domain.exception.SampleItemAlreadyExistsException;
import dev.fabian.gallardo.student.domain.model.SampleItem;
import dev.fabian.gallardo.student.domain.port.out.SampleItemRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;

/** El CRUD contra PostgreSQL real (Testcontainers): tabla, restriccion unica, orden, paginacion y auditoria. */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class SampleItemRepositoryIntegrationTest {

    private final SampleItemRepository repository;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    @Autowired
    SampleItemRepositoryIntegrationTest(SampleItemRepository repository, JdbcTemplate jdbc, TransactionTemplate transaction) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.transaction = transaction;
    }

    @BeforeEach
    void cleanTable() {
        // El pool trabaja con auto-commit apagado: sin transaccion el delete nunca se confirma.
        transaction.executeWithoutResult(_ -> jdbc.update("delete from sample_item"));
    }

    private static SampleItem item(String name) {
        return new SampleItem(UUID.randomUUID(), name, "desc", 4, new BigDecimal("19.90"));
    }

    @Test
    void save_whenNewItem_thenPersistedWithAuditColumns() {
        var saved = repository.save(item("teclado"));

        assertThat(repository.findById(saved.id())).contains(saved);
        var row = jdbc.queryForMap("select created_by, created_date, price from sample_item where id = ?", saved.id());
        assertThat(row.get("created_by")).isNotNull();
        assertThat(row.get("created_date")).isNotNull();
        assertThat((BigDecimal) row.get("price")).isEqualByComparingTo("19.90");
    }

    @Test
    void save_whenExistingId_thenUpdatesTheSameRowAndKeepsCreationAudit() {
        var saved = repository.save(item("mouse"));
        var createdBy = jdbc.queryForObject("select created_by from sample_item where id = ?", String.class, saved.id());

        repository.save(new SampleItem(saved.id(), "mouse gamer", null, 9, BigDecimal.TEN));

        assertThat(jdbc.queryForObject("select count(*) from sample_item", Long.class)).isEqualTo(1L);
        assertThat(repository.findById(saved.id())).get().extracting(SampleItem::name).isEqualTo("mouse gamer");
        assertThat(jdbc.queryForObject("select created_by from sample_item where id = ?", String.class, saved.id()))
                .isEqualTo(createdBy);
        assertThat(jdbc.queryForObject("select last_modified_date from sample_item where id = ?", java.sql.Timestamp.class, saved.id()))
                .isNotNull();
    }

    @Test
    void save_whenNameAlreadyUsedByAnotherItem_thenThrowsConflict() {
        repository.save(item("repetido"));

        assertThatThrownBy(() -> repository.save(item("repetido"))).isInstanceOf(SampleItemAlreadyExistsException.class);
    }

    @Test
    void findAll_whenSeveralItems_thenSortedByNameAndPaged() {
        repository.save(item("c"));
        repository.save(item("a"));
        repository.save(item("b"));

        var first = repository.findAll(0, 2);
        var second = repository.findAll(1, 2);

        assertThat(first.content()).extracting(SampleItem::name).containsExactly("a", "b");
        assertThat(second.content()).extracting(SampleItem::name).containsExactly("c");
        assertThat(first.totalElements()).isEqualTo(3);
    }

    @Test
    void findByName_whenExistsOrNot_thenFindsIt() {
        var saved = repository.save(item("buscable"));

        assertThat(repository.findByName("buscable")).contains(saved);
        assertThat(repository.findByName("nada")).isEmpty();
    }

    @Test
    void deleteById_whenExists_thenRemovesRowAndReportsIt() {
        var saved = repository.save(item("borrable"));

        assertThat(repository.deleteById(saved.id())).isTrue();
        assertThat(repository.deleteById(saved.id())).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from sample_item", Long.class)).isZero();
    }
}
