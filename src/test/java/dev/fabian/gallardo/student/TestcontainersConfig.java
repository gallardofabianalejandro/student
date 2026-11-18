package dev.fabian.gallardo.student;

import static org.testcontainers.utility.DockerImageName.parse;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Dependencias reales (Testcontainers) segun las capacidades del servicio; las comparten los tests y TestApplication. */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(parse("postgres:16-alpine"));
    }
}
