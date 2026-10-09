package com.wifisense;

import com.wifisense.model.Location;
import com.wifisense.model.Measurement;
import com.wifisense.model.Network;
import com.wifisense.model.NetworkStatus;
import com.wifisense.model.Zone;
import com.wifisense.network.DataSourceType;
import com.wifisense.network.NetworkSnapshot;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;

public final class TestData {

    public static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

    private TestData() {
    }

    public static Network network(long id, NetworkStatus status) {
        Zone zone = new Zone(new Location("Main Campus", null, "Pasto"), "Library", 1);
        Network network = new Network(zone, "WiFiSense-Test", "A4:2B:B0:10:00:01", "5GHz", 36,
                Network.SecurityType.WPA3, DataSourceType.SIMULATION);
        ReflectionTestUtils.setField(network, "id", id);
        ReflectionTestUtils.setField(network, "status", status);
        return network;
    }

    public static NetworkSnapshot snapshot(double latency, double jitter, double loss, Integer signal, Instant at) {
        return new NetworkSnapshot(latency, jitter, loss, 300.0, signal, 20, 250.0, 150_000L, Map.of("HTTPS", 100L),
                at, true);
    }

    public static Measurement measurement(Network network, double latency, double jitter, double loss, Integer signal,
                                          Instant at) {
        return Measurement.from(network, snapshot(latency, jitter, loss, signal, at), DataSourceType.SIMULATION);
    }
}
