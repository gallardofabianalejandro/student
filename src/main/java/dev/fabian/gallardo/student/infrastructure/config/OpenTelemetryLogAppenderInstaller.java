package dev.fabian.gallardo.student.infrastructure.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Conecta el appender de logback (logback-spring.xml) con la instancia de OpenTelemetry de Boot para exportar logs.
 * No es perezoso: el perfil local usa lazy-initialization y nadie pide este bean.
 */
@Component
@Lazy(false)
class OpenTelemetryLogAppenderInstaller implements InitializingBean {

    private final OpenTelemetry openTelemetry;

    OpenTelemetryLogAppenderInstaller(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(openTelemetry);
    }
}
