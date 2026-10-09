package com.wifisense.network;

import com.wifisense.network.decorator.DataSourceMetrics;
import com.wifisense.network.decorator.LoggingDecorator;
import com.wifisense.network.decorator.MetricsDecorator;
import com.wifisense.network.decorator.ValidationDecorator;

/**
 * Factory Method creator. Subclasses decide which concrete data source to instantiate;
 * this class applies the same decorator chain to every product.
 */
public abstract class DataSourceCreator {

    public abstract DataSourceType type();

    protected abstract NetworkDataSource createDataSource();

    public final NetworkDataSource create(DataSourceMetrics metrics) {
        NetworkDataSource source = createDataSource();
        return new MetricsDecorator(new LoggingDecorator(new ValidationDecorator(source)), metrics);
    }
}
