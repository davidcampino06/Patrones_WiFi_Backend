package com.wifisense.dto;

import java.time.Instant;

public record NetworkReport(Long networkId, String ssid, Instant from, Instant to, int measurementCount,
                            MetricSummary latencyMs, MetricSummary jitterMs, MetricSummary packetLossPct,
                            MetricSummary bandwidthMbps, MetricSummary signalStrengthDbm, long alerts,
                            long analyses, long anomalies, boolean simulatedData) {
}
