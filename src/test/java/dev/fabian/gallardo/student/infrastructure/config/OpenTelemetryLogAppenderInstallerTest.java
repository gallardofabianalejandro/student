package dev.fabian.gallardo.student.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class OpenTelemetryLogAppenderInstallerTest {

    private static final class CapturingExporter implements LogRecordExporter {
        private final List<LogRecordData> records = new CopyOnWriteArrayList<>();

        @Override
        public CompletableResultCode export(Collection<LogRecordData> logs) {
            records.addAll(logs);
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }

    @Test
    void afterPropertiesSet_whenInstalled_thenLogbackRecordsReachTheOpenTelemetrySdk() {
        var exporter = new CapturingExporter();
        var provider = SdkLoggerProvider.builder()
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
                .build();
        var openTelemetry = OpenTelemetrySdk.builder().setLoggerProvider(provider).build();
        var context = (LoggerContext) LoggerFactory.getILoggerFactory();
        var appender = new OpenTelemetryAppender();
        appender.setContext(context);
        appender.start();
        var logger = context.getLogger("otel.installer.test");
        logger.setAdditive(false);
        logger.addAppender(appender);
        try {
            new OpenTelemetryLogAppenderInstaller(openTelemetry).afterPropertiesSet();

            logger.info("mensaje de prueba {}", 42);

            assertThat(exporter.records).hasSize(1);
            assertThat(exporter.records.get(0).getBodyValue().asString()).isEqualTo("mensaje de prueba 42");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
