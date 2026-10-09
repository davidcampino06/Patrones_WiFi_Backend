package com.wifisense.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Real but partial source: measures connectivity of the host running the backend by timing
 * TCP connections to a probe endpoint. Radio metrics (signal, bandwidth, devices) are not available.
 */
public class SystemDataSource implements NetworkDataSource {

    private final DataSourceProperties.SystemProbe probe;
    private final Clock clock;

    public SystemDataSource(DataSourceProperties.SystemProbe probe, Clock clock) {
        this.probe = probe;
        this.clock = clock;
    }

    @Override
    public DataSourceType type() {
        return DataSourceType.SYSTEM;
    }

    @Override
    public NetworkSnapshot collect(NetworkTarget target) {
        List<Double> latencies = new ArrayList<>();
        for (int attempt = 0; attempt < probe.attempts(); attempt++) {
            probeOnce().ifPresent(latencies::add);
        }
        double loss = 100.0 * (probe.attempts() - latencies.size()) / probe.attempts();
        double latency = latencies.isEmpty()
                ? probe.timeout().toMillis()
                : latencies.stream().mapToDouble(Double::doubleValue).average().orElseThrow();

        return new NetworkSnapshot(latency, jitter(latencies), loss, null, null, null, null, null, null,
                Instant.now(clock), false);
    }

    private Optional<Double> probeOnce() {
        long start = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(probe.host(), probe.port()), (int) probe.timeout().toMillis());
            return Optional.of((System.nanoTime() - start) / 1_000_000.0);
        } catch (IOException unreachable) {
            return Optional.empty();
        }
    }

    /** Mean absolute difference between consecutive samples (RFC 3550 style approximation). */
    static double jitter(List<Double> samples) {
        if (samples.size() < 2) {
            return 0;
        }
        double total = 0;
        for (int i = 1; i < samples.size(); i++) {
            total += Math.abs(samples.get(i) - samples.get(i - 1));
        }
        return total / (samples.size() - 1);
    }
}
