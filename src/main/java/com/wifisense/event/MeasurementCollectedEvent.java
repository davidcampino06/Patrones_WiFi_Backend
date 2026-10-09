package com.wifisense.event;

import com.wifisense.network.DataSourceType;

import java.time.Instant;

public record MeasurementCollectedEvent(long networkId, String ssid, long measurementId, DataSourceType source,
                                        double latencyMs, double packetLossPct, Instant occurredAt)
        implements NetworkEvent {

    @Override
    public String describe() {
        return "Measurement collected from %s: latency %.1f ms, loss %.1f%%".formatted(source, latencyMs, packetLossPct);
    }
}
