package com.wifisense.network;

import java.time.Instant;
import java.util.Map;

/** Immutable reading from a data source. Nullable fields are metrics the source cannot provide. */
public record NetworkSnapshot(
        double latencyMs,
        double jitterMs,
        double packetLossPct,
        Double bandwidthMbps,
        Integer signalStrengthDbm,
        Integer connectedDevices,
        Double trafficVolumeMb,
        Long packetCount,
        Map<String, Long> protocolPackets,
        Instant collectedAt,
        boolean simulated) {

    public NetworkSnapshot {
        protocolPackets = protocolPackets == null ? Map.of() : Map.copyOf(protocolPackets);
    }

    public boolean hasTraffic() {
        return trafficVolumeMb != null && packetCount != null;
    }
}
