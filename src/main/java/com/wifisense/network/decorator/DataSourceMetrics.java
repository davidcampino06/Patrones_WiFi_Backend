package com.wifisense.network.decorator;

import com.wifisense.network.DataSourceType;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/** Thread-safe counters per data source, filled by MetricsDecorator and exposed for observability. */
@Component
public class DataSourceMetrics {

    public record Snapshot(DataSourceType type, long successes, long failures, double averageMillis,
                           Instant lastCallAt) {
    }

    private static final class Counters {
        final LongAdder successes = new LongAdder();
        final LongAdder failures = new LongAdder();
        final LongAdder totalNanos = new LongAdder();
        final AtomicReference<Instant> lastCall = new AtomicReference<>();
    }

    private final Map<DataSourceType, Counters> counters = new ConcurrentHashMap<>();
    private final Clock clock;

    public DataSourceMetrics(Clock clock) {
        this.clock = clock;
    }

    void recordSuccess(DataSourceType type, long nanos) {
        record(type, nanos).successes.increment();
    }

    void recordFailure(DataSourceType type, long nanos) {
        record(type, nanos).failures.increment();
    }

    public List<Snapshot> snapshot() {
        return counters.entrySet().stream()
                .map(entry -> toSnapshot(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Snapshot::type))
                .toList();
    }

    private Counters record(DataSourceType type, long nanos) {
        Counters c = counters.computeIfAbsent(type, key -> new Counters());
        c.totalNanos.add(nanos);
        c.lastCall.set(Instant.now(clock));
        return c;
    }

    private static Snapshot toSnapshot(DataSourceType type, Counters c) {
        long calls = c.successes.sum() + c.failures.sum();
        double average = calls == 0 ? 0 : c.totalNanos.sum() / 1_000_000.0 / calls;
        return new Snapshot(type, c.successes.sum(), c.failures.sum(), Math.round(average * 100) / 100.0,
                c.lastCall.get());
    }
}
