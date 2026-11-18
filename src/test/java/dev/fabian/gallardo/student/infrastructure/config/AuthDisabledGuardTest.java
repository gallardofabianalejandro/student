package dev.fabian.gallardo.student.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuthDisabledGuardTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(AuthDisabledGuard.class);

    @Test
    void guard_whenLoginIsEnabledByDefault_thenIsNotRegistered() {
        runner.run(context -> assertThat(context).doesNotHaveBean(AuthDisabledGuard.class));
    }

    @Test
    void guard_whenLoginIsOffInLocal_thenStartsAndWarns() {
        runner.withPropertyValues("app.auth.enabled=false", "spring.profiles.active=local")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(AuthDisabledGuard.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"production", "prod", "local,production"})
    void guard_whenLoginIsOffInProduction_thenFailsToStart(String profiles) {
        runner.withPropertyValues("app.auth.enabled=false", "spring.profiles.active=" + profiles)
                .run(context -> assertThat(context).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("production"));
    }

    @Test
    void guard_whenLoginIsExplicitlyOn_thenIsNotRegisteredEvenInProduction() {
        runner.withPropertyValues("app.auth.enabled=true", "spring.profiles.active=production")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(AuthDisabledGuard.class));
    }
}
