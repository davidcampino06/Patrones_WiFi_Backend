package com.wifisense.network.decorator;

import com.wifisense.network.NetworkDataSource;
import com.wifisense.network.NetworkSnapshot;
import com.wifisense.network.NetworkTarget;

public class MetricsDecorator extends DataSourceDecorator {

    private final DataSourceMetrics metrics;

    public MetricsDecorator(NetworkDataSource delegate, DataSourceMetrics metrics) {
        super(delegate);
        this.metrics = metrics;
    }

    @Override
    public NetworkSnapshot collect(NetworkTarget target) {
        long start = System.nanoTime();
        try {
            NetworkSnapshot snapshot = delegate.collect(target);
            metrics.recordSuccess(type(), System.nanoTime() - start);
            return snapshot;
        } catch (RuntimeException e) {
            metrics.recordFailure(type(), System.nanoTime() - start);
            throw e;
        }
    }
}
