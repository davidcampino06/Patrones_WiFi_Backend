package com.wifisense.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("wifisense.analysis")
public record AnalysisProperties(@DefaultValue("50") int windowSize, @DefaultValue Thresholds thresholds) {

    public record Thresholds(
            @DefaultValue("60") double latencyWarningMs,
            @DefaultValue("150") double latencyCriticalMs,
            @DefaultValue("15") double jitterWarningMs,
            @DefaultValue("40") double jitterCriticalMs,
            @DefaultValue("2") double packetLossWarningPct,
            @DefaultValue("8") double packetLossCriticalPct,
            @DefaultValue("-70") double signalWarningDbm,
            @DefaultValue("-82") double signalCriticalDbm) {
    }
}
