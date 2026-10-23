package com.wifisense.network;

import com.wifisense.network.decorator.DataSourceMetrics;
import com.wifisense.network.decorator.InvalidSnapshotException;
import com.wifisense.network.decorator.LoggingDecorator;
import com.wifisense.network.decorator.MetricsDecorator;
import com.wifisense.network.decorator.ValidationDecorator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.ZoneOffset;

import static com.wifisense.TestData.NOW;
import static com.wifisense.TestData.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Template Method inside the Factory Method: {@code DataSourceCreator.create()} is the fixed algorithm (make the product,
 * then wrap it in validation, logging and metrics, in that order) and {@code createDataSource()} is the only step a
 * subclass changes. A new source therefore can never skip the validation.
 */
class DataSourceCreatorTemplateTest {

    private static final NetworkTarget TARGET = new NetworkTarget(1, "Test", "A4:2B:B0:10:00:01", "5GHz");

    private final DataSourceMetrics metrics = new DataSourceMetrics(Clock.fixed(NOW, ZoneOffset.UTC));

    /** A new kind of source written by someone who forgot to validate: the template validates anyway. */
    private static final class CarelessCreator extends DataSourceCreator {
        private final NetworkSnapshot reading;

        CarelessCreator(NetworkSnapshot reading) {
            this.reading = reading;
        }

        @Override
        public DataSourceType type() {
            return DataSourceType.SYSTEM;
        }

        @Override
        protected NetworkDataSource createDataSource() {
            return new NetworkDataSource() {
                public DataSourceType type() { return DataSourceType.SYSTEM; }
                public NetworkSnapshot collect(NetworkTarget target) { return reading; }
            };
        }
    }

    @Test
    void theFixedStepsWrapEveryProductInValidationLoggingAndMetricsInThatOrder() {
        NetworkDataSource source = new CarelessCreator(snapshot(20, 2, 0.5, -50, NOW)).create(metrics);

        assertThat(source).isInstanceOf(MetricsDecorator.class);
        Object logging = ReflectionTestUtils.getField(source, "delegate");
        assertThat(logging).isInstanceOf(LoggingDecorator.class);
        assertThat(ReflectionTestUtils.getField(logging, "delegate")).isInstanceOf(ValidationDecorator.class);
        assertThat(source.type()).isEqualTo(DataSourceType.SYSTEM);
    }

    @Test
    void aSubclassCannotSkipTheValidationOrTheMetrics() throws Exception {
        NetworkDataSource source = new CarelessCreator(snapshot(20, 2, 140, -50, NOW)).create(metrics);

        assertThatThrownBy(() -> source.collect(TARGET)).isInstanceOf(InvalidSnapshotException.class);
        assertThat(metrics.snapshot()).singleElement()
                .satisfies(m -> assertThat(m.failures()).isEqualTo(1));
        assertThat(Modifier.isFinal(DataSourceCreator.class.getMethod("create", DataSourceMetrics.class).getModifiers()))
                .as("create() is final: the order of the steps cannot be overridden").isTrue();
    }
}
