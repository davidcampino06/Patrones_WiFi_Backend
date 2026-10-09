package com.wifisense.network;

/** Planned: packet capture sensor producing traffic sessions and protocol statistics. Not implemented yet. */
public class TrafficCaptureDataSource implements NetworkDataSource {

    @Override
    public DataSourceType type() {
        return DataSourceType.TRAFFIC_CAPTURE;
    }

    @Override
    public NetworkSnapshot collect(NetworkTarget target) {
        throw new DataSourceUnavailableException("Traffic capture data source is planned but not implemented yet");
    }
}
