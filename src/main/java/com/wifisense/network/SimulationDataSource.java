package com.wifisense.network;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Generates realistic, clearly simulated readings so the system works without a physical router.
 * Each network gets a stable baseline (derived from its id and band) plus random noise,
 * and occasionally a degradation episode to exercise analysis and alerts.
 */
public class SimulationDataSource implements NetworkDataSource {

    private static final Map<String, Double> PROTOCOL_SHARES = Map.of(
            "HTTPS", 0.62, "QUIC", 0.18, "DNS", 0.08, "HTTP", 0.05, "OTHER", 0.07);

    private final Random random;
    private final double anomalyProbability;
    private final Clock clock;

    public SimulationDataSource(Random random, double anomalyProbability, Clock clock) {
        this.random = random;
        this.anomalyProbability = anomalyProbability;
        this.clock = clock;
    }

    @Override
    public DataSourceType type() {
        return DataSourceType.SIMULATION;
    }

    @Override
    public NetworkSnapshot collect(NetworkTarget target) {
        Baseline base = Baseline.of(target);
        boolean degraded = random.nextDouble() < anomalyProbability;
        double impact = degraded ? 4 + random.nextDouble() * 4 : 1;

        double latency = noisy(base.latency) * impact;
        double jitter = noisy(base.jitter) * impact;
        double loss = Math.min(100, base.loss * random.nextDouble() * 2 + (degraded ? 5 + random.nextDouble() * 15 : 0));
        double bandwidth = noisy(base.bandwidth) / impact;
        int signal = (int) Math.max(-100, base.signal - random.nextInt(8) - (degraded ? 15 : 0));
        int devices = base.devices + random.nextInt(10);
        long packets = 60_000L + random.nextLong(240_000L);

        return new NetworkSnapshot(round(latency), round(jitter), round(loss), round(bandwidth), signal, devices,
                round(packets / 700.0), packets, splitByProtocol(packets), Instant.now(clock), true);
    }

    private double noisy(double value) {
        return value * (0.85 + random.nextDouble() * 0.3);
    }

    private static Map<String, Long> splitByProtocol(long packets) {
        Map<String, Long> result = new LinkedHashMap<>();
        PROTOCOL_SHARES.forEach((protocol, share) -> result.put(protocol, Math.round(packets * share)));
        return result;
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private record Baseline(double latency, double jitter, double loss, double bandwidth, int signal, int devices) {

        static Baseline of(NetworkTarget target) {
            int variant = (int) (target.networkId() % 4);
            return switch (target.frequencyBand()) {
                case "2.4GHz" -> new Baseline(35 + variant * 5, 8, 1.0, 60, -65, 40);
                case "6GHz" -> new Baseline(10 + variant, 2, 0.1, 700, -48, 15);
                default -> new Baseline(18 + variant * 2, 4, 0.4, 300, -55, 25);
            };
        }
    }
}
