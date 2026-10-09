package com.wifisense.analysis;

import com.wifisense.model.Measurement;
import com.wifisense.model.NetworkStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Compares the latest measurement against configured warning/critical limits. */
@Component
public class ThresholdAnalysisStrategy implements AnalysisStrategy {

    /** best = ideal value; works for "higher is worse" and "lower is worse" metrics alike. */
    private record Rule(String metric, Function<Measurement, Number> value, double best, double warning,
                        double critical) {

        private boolean exceeds(double observed, double limit) {
            return limit > best ? observed >= limit : observed <= limit;
        }

        NetworkStatus level(double observed) {
            if (exceeds(observed, critical)) return NetworkStatus.CRITICAL;
            if (exceeds(observed, warning)) return NetworkStatus.WARNING;
            return NetworkStatus.NORMAL;
        }

        double severity(double observed) {
            return Math.max(0, Math.min(1, (observed - best) / (critical - best)));
        }
    }

    private final List<Rule> rules;

    public ThresholdAnalysisStrategy(AnalysisProperties properties) {
        AnalysisProperties.Thresholds t = properties.thresholds();
        this.rules = List.of(
                new Rule("latencia", Measurement::getLatencyMs, 0, t.latencyWarningMs(), t.latencyCriticalMs()),
                new Rule("jitter", Measurement::getJitterMs, 0, t.jitterWarningMs(), t.jitterCriticalMs()),
                new Rule("pérdida de paquetes", Measurement::getPacketLossPct, 0, t.packetLossWarningPct(),
                        t.packetLossCriticalPct()),
                new Rule("señal", Measurement::getSignalStrengthDbm, -30, t.signalWarningDbm(),
                        t.signalCriticalDbm()));
    }

    @Override
    public AnalysisType type() {
        return AnalysisType.THRESHOLD;
    }

    @Override
    public String description() {
        return "Compara la última medición con límites fijos de advertencia y crítico";
    }

    @Override
    public AnalysisOutcome analyze(AnalysisContext context) {
        Measurement latest = context.latest();
        Map<String, NetworkStatus> violations = new LinkedHashMap<>();
        NetworkStatus status = NetworkStatus.NORMAL;
        double score = 0;

        for (Rule rule : rules) {
            Number observed = rule.value().apply(latest);
            if (observed == null) {
                continue;
            }
            NetworkStatus level = rule.level(observed.doubleValue());
            if (level != NetworkStatus.NORMAL) {
                violations.put(rule.metric() + "=" + observed, level);
            }
            status = NetworkStatus.worst(status, level);
            score = Math.max(score, rule.severity(observed.doubleValue()));
        }
        return AnalysisOutcome.of(status, score, summary(violations));
    }

    private static String summary(Map<String, NetworkStatus> violations) {
        if (violations.isEmpty()) {
            return "Todas las métricas están dentro de los límites configurados";
        }
        return "Límites superados: " + violations.entrySet().stream()
                .map(e -> e.getKey() + " (" + e.getValue().label() + ")")
                .collect(Collectors.joining(", "));
    }
}
