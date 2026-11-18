package dev.fabian.gallardo.student;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class ApplicationContextTest {

    private final ApplicationContext context;

    ApplicationContextTest(ApplicationContext context) {
        this.context = context;
    }

    @Test
    void contextLoads_whenStarted_thenApplicationBeanExists() {
        assertThat(context.getBeansOfType(Application.class)).isNotEmpty();
    }
}
