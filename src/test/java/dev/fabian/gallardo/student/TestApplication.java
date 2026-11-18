package dev.fabian.gallardo.student;

import org.springframework.boot.SpringApplication;

/** Arranca la app con dependencias reales via Testcontainers (sin compose). Uso: `task run_tc`. */
class TestApplication {

    public static void main(String[] args) {
        SpringApplication.from(Application::main)
                .with(TestcontainersConfig.class)
                .run(args);
    }
}
