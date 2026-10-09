package com.wifisense.network;

import org.springframework.stereotype.Component;

@Component
public class TrafficCaptureDataSourceCreator extends DataSourceCreator {

    @Override
    public DataSourceType type() {
        return DataSourceType.TRAFFIC_CAPTURE;
    }

    @Override
    protected NetworkDataSource createDataSource() {
        return new TrafficCaptureDataSource();
    }
}
