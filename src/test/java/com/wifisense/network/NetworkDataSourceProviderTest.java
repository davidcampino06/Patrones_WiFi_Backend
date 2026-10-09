package com.wifisense.network;

import com.wifisense.network.decorator.DataSourceMetrics;
import com.wifisense.network.decorator.MetricsDecorator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NetworkDataSourceProviderTest {

    private final DataSourceMetrics metrics = new DataSourceMetrics(Clock.systemUTC());
    private final NetworkDataSourceProvider provider = new NetworkDataSourceProvider(
            List.of(new SnmpDataSourceCreator(), new RouterApiDataSourceCreator()), metrics);

    @Test
    void createsDecoratedSourceForRequestedType() {
        NetworkDataSource source = provider.forType(DataSourceType.SNMP);

        assertThat(source).isInstanceOf(MetricsDecorator.class);
        assertThat(source.type()).isEqualTo(DataSourceType.SNMP);
    }

    @Test
    void reusesTheSameInstancePerType() {
        assertThat(provider.forType(DataSourceType.SNMP)).isSameAs(provider.forType(DataSourceType.SNMP));
    }

    @Test
    void failsClearlyWhenNoCreatorIsRegistered() {
        assertThatThrownBy(() -> provider.forType(DataSourceType.SYSTEM))
                .isInstanceOf(DataSourceUnavailableException.class);
    }

    @Test
    void systemJitterIsMeanConsecutiveDifference() {
        assertThat(SystemDataSource.jitter(List.of(10.0, 14.0, 12.0))).isEqualTo(3.0);
        assertThat(SystemDataSource.jitter(List.of(10.0))).isZero();
    }
}
