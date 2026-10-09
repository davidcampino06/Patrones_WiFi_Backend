package com.wifisense.event;

import com.wifisense.model.NetworkStatus;

import java.time.Instant;

public record NetworkStatusChangedEvent(long networkId, String ssid, NetworkStatus previous, NetworkStatus current,
                                        Long analysisResultId, String reason, Instant occurredAt)
        implements NetworkEvent {

    @Override
    public String describe() {
        return "Status changed %s -> %s: %s".formatted(previous, current, reason);
    }
}
