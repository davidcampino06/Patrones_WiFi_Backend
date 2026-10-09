package com.wifisense.analysis;

import com.wifisense.model.Measurement;
import com.wifisense.model.NetworkStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Flags the latest measurement when it deviates strongly (z-score) from the network's own history. */
@Component
public class StatisticalAnalysisStrategy implements AnalysisStrategy {

    static final int MIN_SAMPLES = 10;
    private static final double WARNING_Z = 2.0;
    private static final double CRITICAL_Z = 3.0;

    /** direction = +1 when higher values are harmful, -1 when lower values are harmful. */
    private record Metric(String name, Function<Measurement, Number> value, int direction) {
    }

    private static final List<Metric> METRICS = List.of(
            new Metric("latency", Measurement::getLatencyMs, 1),
            new Metric("jitter", Measurement::getJitterMs, 1),
            new Metric("packet loss", Measurement::getPacketLossPct, 1),
            new Metric("bandwidth", Measurement::getBandwidthMbps, -1),
            new Metric("signal", Measurement::getSignalStrengthDbm, -1));

    @Override
    public AnalysisType type() {
        return AnalysisType.STATISTICAL;
    }

    @Override
    public String description() {
        return "Z-score of the latest measurement against the network's recent history";
    }

    @Override
    public AnalysisOutcome analyze(AnalysisContext context) {
        if (context.history().size() < MIN_SAMPLES) {
            throw new InsufficientDataException("Statistical analysis needs at least " + MIN_SAMPLES + " measurements");
        }
        Map<String, Double> deviations = new TreeMap<>();
        for (Metric metric : METRICS) {
            Number latest = metric.value().apply(context.latest());
            List<Double> baseline = context.baseline().stream()
                    .map(metric.value()).filter(Objects::nonNull).map(Number::doubleValue).toList();
            if (latest != null && baseline.size() >= MIN_SAMPLES - 1) {
                deviations.put(metric.name(), harmfulZScore(latest.doubleValue(), baseline, metric.direction()));
            }
        }
        double maxZ = deviations.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        NetworkStatus status = maxZ >= CRITICAL_Z ? NetworkStatus.CRITICAL
                : maxZ >= WARNING_Z ? NetworkStatus.WARNING : NetworkStatus.NORMAL;
        return AnalysisOutcome.of(status, maxZ / 4, summary(deviations));
    }

    static double harmfulZScore(double value, List<Double> baseline, int direction) {
        double mean = baseline.stream().mapToDouble(Double::doubleValue).average().orElse(value);
        double variance = baseline.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum() / baseline.size();
        double std = Math.sqrt(variance);
        if (std == 0) {
            return 0;
        }
        return Math.max(0, direction * (value - mean) / std);
    }

    private static String summary(Map<String, Double> deviations) {
        String unusual = deviations.entrySet().stream()
                .filter(e -> e.getValue() >= WARNING_Z)
                .map(e -> "%s z=%.1f".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(", "));
        return unusual.isEmpty() ? "Latest measurement consistent with recent history"
                : "Unusual deviation from history: " + unusual;
    }
}
