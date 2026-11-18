package dev.fabian.gallardo.student;

import static org.assertj.core.api.Assertions.assertThat;

import dev.fabian.gallardo.student.domain.port.out.SampleItemRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/** El perfil local suma classpath:db/seed; aca se activa esa ubicacion para comprobar que el seed carga y es idempotente. */
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed")
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class LocalSeedIntegrationTest {

    private final SampleItemRepository repository;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    @Autowired
    LocalSeedIntegrationTest(SampleItemRepository repository, JdbcTemplate jdbc, TransactionTemplate transaction) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.transaction = transaction;
    }

    @Test
    void seed_whenSeedLocationIsActive_thenSampleDataIsLoadedByFlyway() {
        var page = repository.findAll(0, 10);

        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.content()).extracting(item -> item.name()).contains("Teclado mecanico");
    }

    @Test
    void seed_whenScriptRunsAgain_thenDoesNotDuplicateRows() throws IOException {
        var sql = new String(new ClassPathResource("db/seed/R__seed_sample_item.sql").getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        transaction.executeWithoutResult(_ -> jdbc.execute(sql));

        assertThat(jdbc.queryForObject("select count(*) from sample_item", Long.class)).isEqualTo(3L);
    }
}
