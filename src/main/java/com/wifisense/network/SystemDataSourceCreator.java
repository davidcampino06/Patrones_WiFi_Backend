package com.wifisense.network;

import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class SystemDataSourceCreator extends DataSourceCreator {

    private final DataSourceProperties properties;
    private final Clock clock;

    public SystemDataSourceCreator(DataSourceProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public DataSourceType type() {
        return DataSourceType.SYSTEM;
    }

    @Override
    protected NetworkDataSource createDataSource() {
        return new SystemDataSource(properties.systemProbe(), clock);
    }
}
